package org.gotson.komga.domain.service

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureTestDatabase
class SeriesLifecycleTest(
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val libraryRepository: LibraryRepository
) {

  private var library = makeLibrary()

  @BeforeAll
  fun `setup library`() {
    library = libraryRepository.insert(library)
  }

  @AfterAll
  fun `teardown library`() {
    libraryRepository.deleteAll()
  }

  @AfterEach
  fun `clear repository`() {
    bookRepository.deleteAll()
    seriesRepository.deleteAll()
  }

  @Test
  fun `given series with unordered books when saving then books are ordered with natural sort`() {
    // given
    val books = listOf(
      makeBook("book 1"),
      makeBook("book 05"),
      makeBook("book 6"),
      makeBook("book 002")
    ).also { books -> books.forEach { it.libraryId = library.id } }
    val series = makeSeries(name = "series").also { it.libraryId = library.id }

    // when
    seriesLifecycle.createSeries(series, books)

    // then
    assertThat(seriesRepository.count()).isEqualTo(1)
    assertThat(bookRepository.count()).isEqualTo(4)

    val savedBooks = bookRepository.findBySeriesId(series.id).sortedBy { it.number }
    assertThat(savedBooks.map { it.name }).containsExactly("book 1", "book 002", "book 05", "book 6")
    assertThat(savedBooks.map { it.number }).containsExactly(1, 2, 3, 4)
  }

  @Test
  fun `given series when removing a book then remaining books are indexed in sequence`() {
    // given
    val books = listOf(
      makeBook("book 1"),
      makeBook("book 2"),
      makeBook("book 3"),
      makeBook("book 4")
    ).also { books -> books.forEach { it.libraryId = library.id } }
    val series = makeSeries(name = "series").also { it.libraryId = library.id }
    seriesLifecycle.createSeries(series, books)

    // when
    val book = bookRepository.findBySeriesId(series.id).first { it.name == "book 2" }
    seriesLifecycle.removeBooksFromSeries(series, listOf(book))

    // then
    assertThat(seriesRepository.count()).isEqualTo(1)
    assertThat(bookRepository.count()).isEqualTo(3)

    val savedBooks = bookRepository.findBySeriesId(series.id).sortedBy { it.number }
    assertThat(savedBooks.map { it.name }).containsExactly("book 1", "book 3", "book 4")
    assertThat(savedBooks.map { it.number }).containsExactly(1, 2, 3)
  }

  @Test
  @Transactional
  fun `given series when adding a book then all books are indexed in sequence`() {
    // given
    val books = listOf(
      makeBook("book 1"),
      makeBook("book 2"),
      makeBook("book 4"),
      makeBook("book 5")
    ).also { books -> books.forEach { it.libraryId = library.id } }
    val series = makeSeries(name = "series").also { it.libraryId = library.id }
    seriesLifecycle.createSeries(series, books)

    // when
    val book = makeBook("book 3").also { it.libraryId = library.id }
    val existingBooks = bookRepository.findBySeriesId(series.id)
    seriesLifecycle.updateBooksForSeries(series, listOf(book) + existingBooks)

    // then
    assertThat(seriesRepository.count()).isEqualTo(1)
    assertThat(bookRepository.count()).isEqualTo(5)

    val savedBooks = bookRepository.findBySeriesId(series.id).sortedBy { it.number }
    assertThat(savedBooks.map { it.name }).containsExactly("book 1", "book 2", "book 3", "book 4", "book 5")
    assertThat(savedBooks.map { it.number }).containsExactly(1, 2, 3, 4, 5)
  }
}
