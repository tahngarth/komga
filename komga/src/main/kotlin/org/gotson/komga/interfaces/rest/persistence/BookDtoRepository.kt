package org.gotson.komga.interfaces.rest.persistence

import org.gotson.komga.domain.model.Media
import org.gotson.komga.interfaces.rest.dto.BookDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookDtoRepository {
  fun findAll(search: BookSearch, pageable: Pageable): Page<BookDto>
  fun findByIdOrNull(bookId: Long): BookDto?
  fun findPreviousInSeries(bookId: Long): BookDto?
  fun findNextInSeries(bookId: Long): BookDto?

  // to move to BookRepository later on
  fun getLibraryId(bookId: Long): Long?
  fun getThumbnail(bookId: Long): ByteArray?
  fun findFirstIdInSeries(seriesId: Long): Long?
  fun findAllIdBySeriesId(seriesId: Long): Collection<Long>
  fun findAllIdByLibraryId(libraryId: Long): Collection<Long>
  fun findAllId(bookSearch: BookSearch = BookSearch()): Collection<Long>
}

data class BookSearch(
  val libraryIds: Collection<Long> = emptyList(),
  val seriesIds: Collection<Long> = emptyList(),
  val searchTerm: String? = null,
  val mediaStatus: Collection<Media.Status> = emptyList()
)
