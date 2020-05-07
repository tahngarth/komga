package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.net.URL

@Repository
interface BookRepository : JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
  fun findByUrl(url: URL): Book?
}
