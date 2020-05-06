package org.gotson.komga.infrastructure.jooq

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.persistence.LibraryRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureTestDatabase
class KomgaUserDaoTest(
  @Autowired private val komgaUserDao: KomgaUserDao,
  @Autowired private val libraryRepository: LibraryRepository
) {

  private val library = makeLibrary()

  @BeforeAll
  fun setup() {
    libraryRepository.saveAndFlush(library)
  }

  @AfterAll
  fun tearDown() {
    komgaUserDao.findAll().forEach { komgaUserDao.delete(it) }
    libraryRepository.delete(library)

    assertThat(komgaUserDao.count()).isEqualTo(0)
  }

  @Test
  fun `given a user when saving it then it is persisted`() {
    val user = KomgaUser(
      email = "user@example.org",
      password = "password",
      roleAdmin = false,
      sharedLibrariesIds = setOf(library.id),
      sharedAllLibraries = false
    )

    komgaUserDao.save(user)

    val users = komgaUserDao.findAll()

    assertThat(users).hasSize(1)
    with(users.first()) {
      assertThat(email).isEqualTo("user@example.org")
      assertThat(password).isEqualTo("password")
      assertThat(roleAdmin).isEqualTo(false)
      assertThat(sharedLibrariesIds).containsExactly(library.id)
      assertThat(sharedAllLibraries).isEqualTo(false)
    }
  }
}
