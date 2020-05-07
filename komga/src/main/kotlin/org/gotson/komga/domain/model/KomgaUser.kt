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

  fun getAuthorizedLibraryIds(libraryIds: Collection<Long>?) =
    when {
      // limited user & libraryIds are specified: filter on provided libraries intersecting user's authorized libraries
      !sharedAllLibraries && !libraryIds.isNullOrEmpty() -> libraryIds.intersect(sharedLibrariesIds)

      // limited user: filter on user's authorized libraries
      !sharedAllLibraries -> sharedLibrariesIds

      // non-limited user: filter on provided libraries
      !libraryIds.isNullOrEmpty() -> libraryIds

      else -> emptyList()
    }

  fun canAccessBook(book: Book): Boolean {
    return sharedAllLibraries || sharedLibrariesIds.any { it == book.series.libraryId }
  }

  fun canAccessSeries(series: Series): Boolean {
    return sharedAllLibraries || sharedLibrariesIds.any { it == series.libraryId }
  }

  fun canAccessLibrary(libraryId: Long): Boolean =
    sharedAllLibraries || sharedLibrariesIds.any { it == libraryId }

  fun canAccessLibrary(library: Library): Boolean {
    return sharedAllLibraries || sharedLibrariesIds.any { it == library.id }
  }
}
