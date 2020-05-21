package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.Media

interface BookRepository {
  fun findByIdOrNull(bookId: Long): Book?
  fun findBySeriesId(seriesId: Long): Collection<Book>
  fun findAll(bookSearch: BookSearch = BookSearch()): Collection<Book>

  fun insert(book: Book): Book
  fun update(book: Book)

  fun delete(bookId: Long)
  fun deleteAll(bookIds: List<Long>)
  fun deleteAll()

  fun count(): Long
}

data class BookSearch(
  val libraryIds: Collection<Long> = emptyList(),
  val seriesIds: Collection<Long> = emptyList(),
  val searchTerm: String? = null,
  val mediaStatus: Collection<Media.Status> = emptyList()
)
