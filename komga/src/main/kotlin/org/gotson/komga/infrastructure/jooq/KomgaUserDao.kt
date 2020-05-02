package org.gotson.komga.infrastructure.jooq

import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.jooq.Sequences.HIBERNATE_SEQUENCE
import org.gotson.komga.jooq.Tables
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.ResultQuery
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class KomgaUserDao(
  private val dsl: DSLContext
) : KomgaUserRepository {
  private val u = Tables.USER
  private val l = Tables.LIBRARY
  private val ul = Tables.USER_LIBRARY_SHARING

  override fun count(): Long = dsl.fetchCount(u).toLong()

  override fun findAll(): Iterable<KomgaUser> =
    selectBase()
      .fetchAndMap()

  override fun findByIdOrNull(id: Long): KomgaUser? =
    selectBase()
      .where(u.ID.equal(id))
      .fetchAndMap()
      .firstOrNull()

  private fun selectBase() =
    dsl
      .select(*u.fields())
      .select(l.ID)
      .from(u)
      .leftJoin(ul).onKey()
      .leftJoin(l).onKey()

  private fun ResultQuery<Record>.fetchAndMap() =
    this.fetchGroups({ it.into(u) }, { it.into(l) })
      .map { (ur, lr) ->
        KomgaUser(
          email = ur.email,
          password = ur.password,
          roleAdmin = ur.roleAdmin,
          sharedLibrariesIds = lr.mapNotNull { it.id }.toSet(),
          sharedAllLibraries = ur.sharedAllLibraries,
          id = ur.id
        )
      }

  override fun save(user: KomgaUser): KomgaUser {
    val id = if (user.id == 0L) dsl.nextval(HIBERNATE_SEQUENCE) else user.id

    dsl.mergeInto(u)
      .using(dsl.selectOne())
      .on(u.ID.eq(id))
      .whenMatchedThenUpdate()
      .set(u.EMAIL, user.email)
      .set(u.PASSWORD, user.password)
      .set(u.ROLE_ADMIN, user.roleAdmin)
      .set(u.SHARED_ALL_LIBRARIES, user.sharedAllLibraries)
      .set(u.LAST_MODIFIED_DATE, LocalDateTime.now())
      .whenNotMatchedThenInsert(u.ID, u.EMAIL, u.PASSWORD, u.ROLE_ADMIN, u.SHARED_ALL_LIBRARIES)
      .values(id, user.email, user.password, user.roleAdmin, user.sharedAllLibraries)
      .execute()

    dsl.deleteFrom(ul)
      .where(ul.USER_ID.eq(id))
      .execute()

    user.sharedLibrariesIds.forEach {
      dsl.insertInto(ul)
        .columns(ul.USER_ID, ul.LIBRARY_ID)
        .values(id, it)
        .execute()
    }

    return findByIdOrNull(id)!!
  }

  override fun saveAll(users: Iterable<KomgaUser>): Iterable<KomgaUser> = users.map { save(it) }

  override fun delete(user: KomgaUser) {
    dsl.deleteFrom(ul)
      .where(ul.USER_ID.equal(user.id))
      .execute()

    dsl.deleteFrom(u)
      .where(u.ID.equal(user.id))
      .execute()
  }

  override fun existsByEmailIgnoreCase(email: String): Boolean =
    dsl.fetchExists(
      dsl.selectFrom(u)
        .where(u.EMAIL.equalIgnoreCase(email))
    )

  override fun findByEmailIgnoreCase(email: String): KomgaUser? =
    selectBase()
      .where(u.EMAIL.equalIgnoreCase(email))
      .fetchAndMap()
      .firstOrNull()

  override fun findBySharedLibrariesContaining(library: Library): List<KomgaUser> =
    selectBase()
      .leftJoin(ul).on(u.ID.eq(ul.USER_ID))
      .where(ul.LIBRARY_ID.equal(library.id))
      .fetchAndMap()

}
