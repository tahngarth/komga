package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.Library
import org.springframework.data.repository.CrudRepository

//@Repository
//interface KomgaUserRepository : CrudRepository<KomgaUser, Long> {
interface KomgaUserRepository {
  fun count(): Long

  fun findAll(): Iterable<KomgaUser>
  fun findByIdOrNull(id: Long): KomgaUser?

  fun save(user: KomgaUser): KomgaUser
  fun saveAll(users: Iterable<KomgaUser>): Iterable<KomgaUser>

  fun delete(user: KomgaUser)

  fun existsByEmailIgnoreCase(email: String): Boolean
  fun findByEmailIgnoreCase(email: String): KomgaUser?
  fun findBySharedLibrariesContaining(library: Library): List<KomgaUser>
}
