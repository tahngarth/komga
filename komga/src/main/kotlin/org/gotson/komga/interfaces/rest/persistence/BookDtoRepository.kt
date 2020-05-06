package org.gotson.komga.interfaces.rest.persistence

import org.gotson.komga.domain.model.Media
import org.gotson.komga.interfaces.rest.dto.BookDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookDtoRepository {
  fun findAll(search: BookSearch, pageable: Pageable): Page<BookDto>
  fun findByIdOrNull(bookId: Long): BookDto?
  fun getLibraryId(bookId: Long): Long?
  fun getThumbnail(bookId: Long): ByteArray?
  fun findPreviousInSeries(bookId: Long): BookDto?
  fun findNextInSeries(bookId: Long): BookDto?
}

data class BookSearch(
  val libraryIds: Collection<Long> = emptyList(),
  val seriesIds: Collection<Long> = emptyList(),
  val searchTerm: String? = null,
  val mediaStatus: Collection<Media.Status> = emptyList()
)
