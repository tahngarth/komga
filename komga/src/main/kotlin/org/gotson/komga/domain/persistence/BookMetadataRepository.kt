package org.gotson.komga.domain.persistence

import org.springframework.data.repository.query.Param

//@Repository
//interface BookMetadataRepository : JpaRepository<BookMetadata, Long> {
interface BookMetadataRepository {
//  @Query(
//    value = "select distinct a.name from BOOK_METADATA_AUTHOR a where a.name ilike CONCAT('%', :search, '%') order by a.name",
//    nativeQuery = true
//  )
  fun findAuthorsByName(@Param("search") search: String): List<String>
}
