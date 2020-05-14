package org.gotson.komga.interfaces.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import mu.KotlinLogging
import org.gotson.komga.application.tasks.TaskReceiver
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.infrastructure.swagger.PageableAsQueryParam
import org.gotson.komga.infrastructure.swagger.PageableWithoutSortAsQueryParam
import org.gotson.komga.interfaces.rest.dto.BookDto
import org.gotson.komga.interfaces.rest.dto.SeriesDto
import org.gotson.komga.interfaces.rest.dto.SeriesMetadataUpdateDto
import org.gotson.komga.interfaces.rest.dto.restrictUrl
import org.gotson.komga.interfaces.rest.persistence.BookDtoRepository
import org.gotson.komga.interfaces.rest.persistence.BookSearch
import org.gotson.komga.interfaces.rest.persistence.SeriesDtoRepository
import org.gotson.komga.interfaces.rest.persistence.SeriesSearch
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("api/v1/series", produces = [MediaType.APPLICATION_JSON_VALUE])
class SeriesController(
  private val seriesRepository: SeriesRepository,
  private val seriesDtoRepository: SeriesDtoRepository,
  private val bookDtoRepository: BookDtoRepository,
  private val bookController: BookController,
  private val taskReceiver: TaskReceiver
) {

  @PageableAsQueryParam
  @GetMapping
  fun getAllSeries(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "search", required = false) searchTerm: String?,
    @RequestParam(name = "library_id", required = false) libraryIds: List<Long>?,
    @RequestParam(name = "status", required = false) metadataStatus: List<SeriesMetadata.Status>?,
    @Parameter(hidden = true) page: Pageable
  ): Page<SeriesDto> {
    val pageRequest = PageRequest.of(
      page.pageNumber,
      page.pageSize,
      if (page.sort.isSorted) Sort.by(page.sort.map { it.ignoreCase() }.toList())
      else Sort.by(Sort.Order.asc("metadata.titleSort").ignoreCase())
    )

    val seriesSearch = SeriesSearch(
      libraryIds = principal.user.getAuthorizedLibraryIds(libraryIds),
      searchTerm = searchTerm,
      metadataStatus = metadataStatus ?: emptyList()
    )

    return seriesDtoRepository.findAll(seriesSearch, pageRequest)
      .map { it.restrictUrl(!principal.user.roleAdmin) }
  }

  @Operation(description = "Return recently added or updated series.")
  @PageableWithoutSortAsQueryParam
  @GetMapping("/latest")
  fun getLatestSeries(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @Parameter(hidden = true) page: Pageable
  ): Page<SeriesDto> {
    val pageRequest = PageRequest.of(
      page.pageNumber,
      page.pageSize,
      Sort.by(Sort.Direction.DESC, "lastModifiedDate")
    )

    val libraryIds = if (principal.user.sharedAllLibraries) emptyList<Long>() else principal.user.sharedLibrariesIds

    return seriesDtoRepository.findAll(
      SeriesSearch(libraryIds = libraryIds),
      pageRequest
    ).map { it.restrictUrl(!principal.user.roleAdmin) }
  }

  @Operation(description = "Return newly added series.")
  @PageableWithoutSortAsQueryParam
  @GetMapping("/new")
  fun getNewSeries(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @Parameter(hidden = true) page: Pageable
  ): Page<SeriesDto> {
    val pageRequest = PageRequest.of(
      page.pageNumber,
      page.pageSize,
      Sort.by(Sort.Direction.DESC, "createdDate")
    )

    val libraryIds = if (principal.user.sharedAllLibraries) emptyList<Long>() else principal.user.sharedLibrariesIds

    return seriesDtoRepository.findAll(
      SeriesSearch(libraryIds = libraryIds),
      pageRequest
    ).map { it.restrictUrl(!principal.user.roleAdmin) }
  }

  @Operation(description = "Return recently updated series, but not newly added ones.")
  @PageableWithoutSortAsQueryParam
  @GetMapping("/updated")
  fun getUpdatedSeries(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @Parameter(hidden = true) page: Pageable
  ): Page<SeriesDto> {
    val pageRequest = PageRequest.of(
      page.pageNumber,
      page.pageSize,
      Sort.by(Sort.Direction.DESC, "lastModifiedDate")
    )

    val libraryIds = if (principal.user.sharedAllLibraries) emptyList<Long>() else principal.user.sharedLibrariesIds

    return seriesDtoRepository.findRecentlyUpdated(
      SeriesSearch(libraryIds = libraryIds),
      pageRequest
    ).map { it.restrictUrl(!principal.user.roleAdmin) }
  }

  @GetMapping("{seriesId}")
  fun getOneSeries(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @PathVariable(name = "seriesId") id: Long
  ): SeriesDto =
    seriesDtoRepository.findByIdOrNull(id)?.let {
      if (!principal.user.canAccessLibrary(it.libraryId)) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
      it.restrictUrl(!principal.user.roleAdmin)
    } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  @ApiResponse(content = [Content(schema = Schema(type = "string", format = "binary"))])
  @GetMapping(value = ["{seriesId}/thumbnail"], produces = [MediaType.IMAGE_JPEG_VALUE])
  fun getSeriesThumbnail(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @PathVariable(name = "seriesId") seriesId: Long
  ): ResponseEntity<ByteArray> =
    bookDtoRepository.findFirstIdInSeries(seriesId)?.let {
      bookController.getBookThumbnail(principal, it)
    } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  @PageableAsQueryParam
  @GetMapping("{seriesId}/books")
  fun getAllBooksBySeries(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @PathVariable(name = "seriesId") id: Long,
    @RequestParam(name = "media_status", required = false) mediaStatus: List<Media.Status>?,
    @Parameter(hidden = true) page: Pageable
  ): Page<BookDto> {
    val libraryId = seriesDtoRepository.getLibraryId(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    if (!principal.user.canAccessLibrary(libraryId)) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

    val pageRequest = PageRequest.of(
      page.pageNumber,
      page.pageSize,
      if (page.sort.isSorted) Sort.by(page.sort.map { it.ignoreCase() }.toList())
      else Sort.by(Sort.Order.asc("metadata.numberSort"))
    )

    return bookDtoRepository.findAll(
      BookSearch(
        seriesIds = listOf(id),
        mediaStatus = mediaStatus ?: emptyList()
      ),
      pageRequest
    ).map { it.restrictUrl(!principal.user.roleAdmin) }
  }

  @PostMapping("{seriesId}/analyze")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun analyze(@PathVariable seriesId: Long) {
    bookDtoRepository.findAllIdBySeriesId(seriesId).forEach {
      taskReceiver.analyzeBook(it)
    }
  }

  @PostMapping("{seriesId}/metadata/refresh")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun refreshMetadata(@PathVariable seriesId: Long) {
    bookDtoRepository.findAllIdBySeriesId(seriesId).forEach {
      taskReceiver.refreshBookMetadata(it)
    }
  }

  @PatchMapping("{seriesId}/metadata")
  @PreAuthorize("hasRole('ADMIN')")
  fun updateMetadata(
    @PathVariable seriesId: Long,
    @Parameter(description = "Metadata fields to update. Set a field to null to unset the metadata. You can omit fields you don't want to update.")
    @Valid @RequestBody newMetadata: SeriesMetadataUpdateDto
  ): SeriesDto =
    seriesRepository.findByIdOrNull(seriesId)?.let { series ->
      with(newMetadata) {
        status?.let { series.metadata.status = it }
        statusLock?.let { series.metadata.statusLock = it }
        title?.let { series.metadata.title = it }
        titleLock?.let { series.metadata.titleLock = it }
        titleSort?.let { series.metadata.titleSort = it }
        titleSortLock?.let { series.metadata.titleSortLock = it }
      }
      val newSeries = seriesRepository.save(series)
      seriesDtoRepository.findByIdOrNull(newSeries.id)!!
    } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

}
