package org.gotson.komga.domain.service

import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

@Service
class SeriesLifecycle(
  private val bookRepository: BookRepository,
  private val seriesRepository: SeriesRepository
) {

  @Transactional
  fun removeBooksFromSeries(series: Series, bookstoRemove: Collection<Book>) {
    val existingBooks = bookRepository.findBySeriesId(series.id)
    var (remove, keep) = existingBooks.partition { book -> bookstoRemove.map { it.url }.contains(book.url) }

    bookRepository.deleteAll(remove)

    keep = keep.sortedWith(compareBy(natSortComparator) { it.name })
    keep.forEachIndexed { index, book -> book.number = index + 1 }

    keep.forEach {
      bookRepository.saveAndFlush(it)
    }
  }

  @Transactional
  fun updateBooksForSeries(series: Series, booksToAdd: Collection<Book>) {
    booksToAdd.forEach {
      check(it.libraryId == series.libraryId) { "Cannot add book to series if they don't share the same libraryId" }
    }

    val existingBooks = bookRepository.findBySeriesId(series.id)
    val remove = existingBooks.filterNot { book -> booksToAdd.map { it.url }.contains(book.url) }

    bookRepository.deleteAll(remove)

    val allBooks = booksToAdd.sortedWith(compareBy(natSortComparator) { it.name })
    allBooks.forEachIndexed { index, book -> book.number = index + 1 }

    // JPA
    allBooks.forEach {
      it.seriesId = series.id
      bookRepository.saveAndFlush(it)
    }


  }

  fun createSeries(series: Series, books: Collection<Book>): Series {
    books.forEach {
      check(it.libraryId == series.libraryId) { "Cannot add book to series if they don't share the same libraryId" }
    }

    val allBooks = books.sortedWith(compareBy(natSortComparator) { it.name })
    allBooks.forEachIndexed { index, book -> book.number = index + 1 }

    val createdSeries = seriesRepository.saveAndFlush(series)

    allBooks.forEach {
      it.seriesId = series.id
      bookRepository.saveAndFlush(it)
    }

    return createdSeries
  }

  fun deleteSeries(series: Series) {
    bookRepository.deleteBySeriesId(series.id)
    seriesRepository.delete(series)
  }
}
