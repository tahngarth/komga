package org.gotson.komga.infrastructure.jooq

import org.gotson.komga.domain.persistence.BookMetadataRepository
import org.gotson.komga.jooq.Tables.BOOK_METADATA_AUTHOR
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class BookMetadataDao(
  private val dslContext: DSLContext
) : BookMetadataRepository{

  override fun findAuthorsByName(search: String): List<String> {
    return dslContext.selectDistinct(BOOK_METADATA_AUTHOR.NAME)
      .from(BOOK_METADATA_AUTHOR)
      .where(BOOK_METADATA_AUTHOR.NAME.likeIgnoreCase("%$search%"))
      .orderBy(BOOK_METADATA_AUTHOR.NAME)
      .fetch(BOOK_METADATA_AUTHOR.NAME)
  }
}
