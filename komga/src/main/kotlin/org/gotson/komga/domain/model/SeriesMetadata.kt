package org.gotson.komga.domain.model

import javax.validation.constraints.NotBlank

//TODO: make immutable data class
class SeriesMetadata : AuditableEntity {
  constructor(
    status: Status = Status.ONGOING,
    title: String,
    titleSort: String = title
  ) : super() {
    this.status = status
    this.title = title
    this.titleSort = titleSort
  }

  var seriesId: Long = 0

  var status: Status

  @NotBlank
  var title: String
    set(value) {
      require(value.isNotBlank()) { "title must not be blank" }
      field = value.trim()
    }

  @NotBlank
  var titleSort: String
    set(value) {
      require(value.isNotBlank()) { "titleSort must not be blank" }
      field = value.trim()
    }


  var statusLock: Boolean = false

  var titleLock: Boolean = false

  var titleSortLock: Boolean = false


  enum class Status {
    ENDED, ONGOING, ABANDONED, HIATUS
  }
}
