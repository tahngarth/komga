package org.gotson.komga.domain.model

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class KomgaUser(
  @Email
  @NotBlank
  val email: String,
  @NotBlank
  val password: String,
  val roleAdmin: Boolean = false,
  val sharedLibrariesIds: Set<Long> = emptySet(),
  val sharedAllLibraries: Boolean = true,
  val id: Long = 0
) : AuditableEntity() {

  fun roles(): Set<String> {
    val roles = mutableSetOf("USER")
    if (roleAdmin) roles.add("ADMIN")
    return roles
  }

  fun canAccessBook(book: Book): Boolean {
    return sharedAllLibraries || sharedLibrariesIds.any { it == book.series.library.id }
  }

  fun canAccessSeries(series: Series): Boolean {
    return sharedAllLibraries || sharedLibrariesIds.any { it == series.library.id }
  }

  fun canAccessLibrary(libraryId: Long): Boolean =
    sharedAllLibraries || sharedLibrariesIds.any { it == libraryId }

  fun canAccessLibrary(library: Library): Boolean {
    return sharedAllLibraries || sharedLibrariesIds.any { it == library.id }
  }
}
