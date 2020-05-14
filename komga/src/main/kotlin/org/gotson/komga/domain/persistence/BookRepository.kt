package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
  fun findBySeriesId(seriesId: Long): Collection<Book>
  fun deleteBySeriesId(seriesId: Long)
}
