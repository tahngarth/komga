package org.gotson.komga.infrastructure.jooq

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.gotson.komga.domain.model.Author
import org.gotson.komga.domain.model.BookMetadata
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
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureTestDatabase
class BookMetadataDaoTest(
  @Autowired private val bookMetadataDao: BookMetadataDao,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val libraryRepository: LibraryRepository
) {
  private var library = makeLibrary()
  private var series = makeSeries("Series")
  private var book = makeBook("Book")

  @BeforeAll
  fun setup() {
    library = libraryRepository.insert(library)

    series.libraryId = library.id
    series = seriesRepository.insert(series)

    book.libraryId = library.id
    book.seriesId = series.id
    book = bookRepository.insert(book)
  }

  @AfterEach
  fun deleteMedia() {
    bookRepository.findAll().forEach {
      bookMetadataDao.delete(it.id)
    }
  }

  @AfterAll
  fun tearDown() {
    bookRepository.deleteAll()
    seriesRepository.deleteAll()
    libraryRepository.deleteAll()
  }

  @Test
  fun `given a metadata when inserting then it is persisted`() {
    val now = LocalDateTime.now()
    val metadata = BookMetadata(
      title = "Book",
      summary = "Summary",
      number = "1",
      numberSort = 1F,
      readingDirection = BookMetadata.ReadingDirection.LEFT_TO_RIGHT,
      publisher = "publisher",
      ageRating = 18,
      releaseDate = LocalDate.now(),
      authors = mutableListOf(Author("author", "role"))
    ).also {
      it.bookId = book.id
      it.titleLock = true
      it.summaryLock = true
      it.numberLock = true
      it.numberSortLock = true
      it.readingDirectionLock = true
      it.publisherLock = true
      it.ageRatingLock = true
      it.releaseDateLock = true
      it.authorsLock = true
    }

    Thread.sleep(5)

    val created = bookMetadataDao.insert(metadata)

    assertThat(created.bookId).isEqualTo(book.id)
    assertThat(created.createdDate).isAfter(now)
    assertThat(created.lastModifiedDate).isAfter(now)

    assertThat(created.title).isEqualTo(metadata.title)
    assertThat(created.summary).isEqualTo(metadata.summary)
    assertThat(created.number).isEqualTo(metadata.number)
    assertThat(created.numberSort).isEqualTo(metadata.numberSort)
    assertThat(created.readingDirection).isEqualTo(metadata.readingDirection)
    assertThat(created.publisher).isEqualTo(metadata.publisher)
    assertThat(created.ageRating).isEqualTo(metadata.ageRating)
    assertThat(created.releaseDate).isEqualTo(metadata.releaseDate)
    assertThat(created.authors).hasSize(1)
    with(created.authors.first()) {
      assertThat(name).isEqualTo(metadata.authors.first().name)
      assertThat(role).isEqualTo(metadata.authors.first().role)
    }

    assertThat(created.titleLock).isEqualTo(metadata.titleLock)
    assertThat(created.summaryLock).isEqualTo(metadata.summaryLock)
    assertThat(created.numberLock).isEqualTo(metadata.numberLock)
    assertThat(created.numberSortLock).isEqualTo(metadata.numberSortLock)
    assertThat(created.readingDirectionLock).isEqualTo(metadata.readingDirectionLock)
    assertThat(created.publisherLock).isEqualTo(metadata.publisherLock)
    assertThat(created.ageRatingLock).isEqualTo(metadata.ageRatingLock)
    assertThat(created.releaseDateLock).isEqualTo(metadata.releaseDateLock)
    assertThat(created.authorsLock).isEqualTo(metadata.authorsLock)
  }

  @Test
  fun `given a minimum metadata when inserting then it is persisted`() {
    val metadata = BookMetadata(
      title = "Book",
      number = "1",
      numberSort = 1F
    ).also {
      it.bookId = book.id
    }

    val created = bookMetadataDao.insert(metadata)

    assertThat(created.bookId).isEqualTo(book.id)

    assertThat(created.title).isEqualTo(metadata.title)
    assertThat(created.summary).isBlank()
    assertThat(created.number).isEqualTo(metadata.number)
    assertThat(created.numberSort).isEqualTo(metadata.numberSort)
    assertThat(created.readingDirection).isNull()
    assertThat(created.publisher).isBlank()
    assertThat(created.ageRating).isNull()
    assertThat(created.releaseDate).isNull()
    assertThat(created.authors).isEmpty()

    assertThat(created.titleLock).isFalse()
    assertThat(created.summaryLock).isFalse()
    assertThat(created.numberLock).isFalse()
    assertThat(created.numberSortLock).isFalse()
    assertThat(created.readingDirectionLock).isFalse()
    assertThat(created.publisherLock).isFalse()
    assertThat(created.ageRatingLock).isFalse()
    assertThat(created.releaseDateLock).isFalse()
    assertThat(created.authorsLock).isFalse()
  }

  @Test
  fun `given existing metadata when updating then it is persisted`() {
    val metadata = BookMetadata(
      title = "Book",
      summary = "Summary",
      number = "1",
      numberSort = 1F,
      readingDirection = BookMetadata.ReadingDirection.LEFT_TO_RIGHT,
      publisher = "publisher",
      ageRating = 18,
      releaseDate = LocalDate.now(),
      authors = mutableListOf(Author("author", "role"))
    ).also {
      it.bookId = book.id
    }
    val created = bookMetadataDao.insert(metadata)

    Thread.sleep(5)

    val modificationDate = LocalDateTime.now()

    with(created) {
      title = "BookUpdated"
      summary = "SummaryUpdated"
      number = "2"
      numberSort = 2F
      readingDirection = BookMetadata.ReadingDirection.RIGHT_TO_LEFT
      publisher = "publisher2"
      ageRating = 15
      releaseDate = LocalDate.now()
      authors = mutableListOf(Author("author2", "role2"))
    }
    created.let {
      it.titleLock = true
      it.summaryLock = true
      it.numberLock = true
      it.numberSortLock = true
      it.readingDirectionLock = true
      it.publisherLock = true
      it.ageRatingLock = true
      it.releaseDateLock = true
      it.authorsLock = true
    }

    bookMetadataDao.update(created)
    val modified = bookMetadataDao.findById(created.bookId)

    assertThat(modified.bookId).isEqualTo(created.bookId)
    assertThat(modified.createdDate).isEqualTo(created.createdDate)
    assertThat(modified.lastModifiedDate)
      .isAfter(modificationDate)
      .isNotEqualTo(created.lastModifiedDate)

    assertThat(modified.title).isEqualTo(created.title)
    assertThat(modified.summary).isEqualTo(created.summary)
    assertThat(modified.number).isEqualTo(created.number)
    assertThat(modified.numberSort).isEqualTo(created.numberSort)
    assertThat(modified.readingDirection).isEqualTo(created.readingDirection)
    assertThat(modified.publisher).isEqualTo(created.publisher)
    assertThat(modified.ageRating).isEqualTo(created.ageRating)

    assertThat(modified.titleLock).isEqualTo(created.titleLock)
    assertThat(modified.summaryLock).isEqualTo(created.summaryLock)
    assertThat(modified.numberLock).isEqualTo(created.numberLock)
    assertThat(modified.numberSortLock).isEqualTo(created.numberSortLock)
    assertThat(modified.readingDirectionLock).isEqualTo(created.readingDirectionLock)
    assertThat(modified.publisherLock).isEqualTo(created.publisherLock)
    assertThat(modified.ageRatingLock).isEqualTo(created.ageRatingLock)
    assertThat(modified.releaseDateLock).isEqualTo(created.releaseDateLock)
    assertThat(modified.authorsLock).isEqualTo(created.authorsLock)

    assertThat(modified.authors.first().name).isEqualTo(created.authors.first().name)
    assertThat(modified.authors.first().role).isEqualTo(created.authors.first().role)
  }

  @Test
  fun `given existing metadata when finding by id then metadata is returned`() {
    val metadata = BookMetadata(
      title = "Book",
      summary = "Summary",
      number = "1",
      numberSort = 1F,
      readingDirection = BookMetadata.ReadingDirection.LEFT_TO_RIGHT,
      publisher = "publisher",
      ageRating = 18,
      releaseDate = LocalDate.now(),
      authors = mutableListOf(Author("author", "role"))
    ).also {
      it.bookId = book.id
    }
    val created = bookMetadataDao.insert(metadata)

    val found = catchThrowable { bookMetadataDao.findById(created.bookId) }

    assertThat(found).doesNotThrowAnyException()
  }

  @Test
  fun `given non-existing metadata when finding by id then exception is thrown`() {
    val found = catchThrowable { bookMetadataDao.findById(128742) }

    assertThat(found).isInstanceOf(Exception::class.java)
  }
}

