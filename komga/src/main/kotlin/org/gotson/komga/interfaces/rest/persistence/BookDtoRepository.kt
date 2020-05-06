package org.gotson.komga.interfaces.rest.persistence

import org.gotson.komga.domain.model.Media
import org.gotson.komga.interfaces.rest.dto.BookDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookDtoRepository {
  fun findAll(search: BookSearch, pageable: Pageable): Page<BookDto>
}

data class BookSearch(
  val libraryIds: Collection<Long> = emptyList(),
  val searchTerm: String?,
  val mediaStatus: Collection<Media.Status> = emptyList(),
  val includeFullUrl: Boolean = false
)
