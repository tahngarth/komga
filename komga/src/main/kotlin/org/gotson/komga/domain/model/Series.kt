package org.gotson.komga.domain.model

import java.net.URL
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

//TODO: make immutable data class
class Series(
  @NotBlank
  var name: String,

  var url: URL,

  var fileLastModified: LocalDateTime
) : AuditableEntity() {
  var id: Long = 0

  @NotNull
  var libraryId: Long = 0L

  override fun toString(): String = "Series($id, ${url.toURI().path})"
}
