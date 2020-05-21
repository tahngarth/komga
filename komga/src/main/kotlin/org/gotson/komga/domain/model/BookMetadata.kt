package org.gotson.komga.domain.model

import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.PositiveOrZero

class BookMetadata : AuditableEntity {
  constructor(
    title: String,
    summary: String = "",
    number: String,
    numberSort: Float,
    readingDirection: ReadingDirection? = null,
    publisher: String = "",
    ageRating: Int? = null,
    releaseDate: LocalDate? = null,
    authors: MutableList<Author> = mutableListOf()
  ) : super() {
    this.title = title
    this.summary = summary
    this.number = number
    this.numberSort = numberSort
    this.readingDirection = readingDirection
    this.publisher = publisher
    this.ageRating = ageRating
    this.releaseDate = releaseDate
    this.authors = authors
  }

  var bookId: Long = 0

  @NotBlank
  var title: String
    set(value) {
      require(value.isNotBlank()) { "title must not be blank" }
      field = value.trim()
    }

  var summary: String
    set(value) {
      field = value.trim()
    }

  @NotBlank
  var number: String
    set(value) {
      require(value.isNotBlank()) { "number must not be blank" }
      field = value.trim()
    }

  var numberSort: Float

  var readingDirection: ReadingDirection?

  var publisher: String
    set(value) {
      field = value.trim()
    }

  @PositiveOrZero
  var ageRating: Int?

  var releaseDate: LocalDate?

  var authors: MutableList<Author>


  var titleLock: Boolean = false

  var summaryLock: Boolean = false

  var numberLock: Boolean = false

  var numberSortLock: Boolean = false

  var readingDirectionLock: Boolean = false

  var publisherLock: Boolean = false

  var ageRatingLock: Boolean = false

  var releaseDateLock: Boolean = false

  var authorsLock: Boolean = false

  enum class ReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    VERTICAL,
    WEBTOON
  }
}
