package org.gotson.komga.interfaces.rest.persistence

import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.interfaces.rest.dto.SeriesDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

interface SeriesDtoRepository {
  fun findAll(search: SeriesSearch, pageable: Pageable): Page<SeriesDto>
  fun findAll(search: SeriesSearch, sort: Sort): Collection<SeriesDto>
  fun findRecentlyUpdated(search: SeriesSearch, pageable: Pageable): Page<SeriesDto>
  fun findByIdOrNull(seriesId: Long): SeriesDto?

  // to move to SeriesRepository later on
  fun getLibraryId(seriesId: Long): Long?
}

data class SeriesSearch(
  val libraryIds: Collection<Long> = emptyList(),
  val searchTerm: String? = null,
  val metadataStatus: Collection<SeriesMetadata.Status> = emptyList()
)
