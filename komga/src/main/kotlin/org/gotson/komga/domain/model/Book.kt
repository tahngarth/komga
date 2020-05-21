package org.gotson.komga.domain.model

import com.jakewharton.byteunits.BinaryByteUnit
import org.apache.commons.io.FilenameUtils
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank

class Book(
  @NotBlank
  var name: String,

  var url: URL,

  var fileLastModified: LocalDateTime,

  var fileSize: Long = 0
) : AuditableEntity() {
  var id: Long = 0

  var seriesId: Long = 0

  var libraryId: Long = 0

  var number: Int = 0


  fun fileName(): String = FilenameUtils.getName(url.toString())

  fun fileExtension(): String = FilenameUtils.getExtension(url.toString())

  fun path(): Path = Paths.get(this.url.toURI())

  fun fileSizeHumanReadable(): String = BinaryByteUnit.format(fileSize)

  override fun toString(): String = "Book($id, ${url.toURI().path})"
}
