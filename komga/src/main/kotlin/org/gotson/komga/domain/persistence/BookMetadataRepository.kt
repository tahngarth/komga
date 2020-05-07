package org.gotson.komga.domain.persistence

import org.springframework.data.repository.query.Param

interface BookMetadataRepository {
  fun findAuthorsByName(@Param("search") search: String): List<String>
}
