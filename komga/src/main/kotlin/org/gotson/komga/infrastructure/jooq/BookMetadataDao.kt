package org.gotson.komga.infrastructure.jooq

import org.gotson.komga.domain.model.Author
import org.gotson.komga.domain.model.BookMetadata
import org.gotson.komga.domain.persistence.BookMetadataRepository
import org.gotson.komga.jooq.Tables
import org.gotson.komga.jooq.tables.records.BookMetadataAuthorRecord
import org.gotson.komga.jooq.tables.records.BookMetadataRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class BookMetadataDao(
  private val dsl: DSLContext
) : BookMetadataRepository {

  private val d = Tables.BOOK_METADATA
  private val a = Tables.BOOK_METADATA_AUTHOR
  private val bt = Tables.BOOK_METADATA_TAG

  private val groupFields = arrayOf(*d.fields(), *a.fields())

  override fun findById(bookId: String): BookMetadata =
    find(dsl, listOf(bookId)).first()

  override fun findByIdOrNull(bookId: String): BookMetadata? =
    find(dsl, listOf(bookId)).firstOrNull()

  override fun findByIds(bookIds: Collection<String>): Collection<BookMetadata> =
    find(dsl, bookIds)

  private fun find(dsl: DSLContext, bookIds: Collection<String>) =
    dsl.select(*groupFields)
      .from(d)
      .leftJoin(a).on(d.BOOK_ID.eq(a.BOOK_ID))
      .where(d.BOOK_ID.`in`(bookIds))
      .groupBy(*groupFields)
      .fetchGroups(
        { it.into(d) }, { it.into(a) }
      ).map { (dr, ar) ->
        dr.toDomain(ar.filterNot { it.name == null }.map { it.toDomain() }, findTags(dr.bookId))
      }

  private fun findTags(bookId: String) =
    dsl.select(bt.TAG)
      .from(bt)
      .where(bt.BOOK_ID.eq(bookId))
      .fetchInto(bt)
      .mapNotNull { it.tag }
      .toSet()

  override fun insert(metadata: BookMetadata) {
    insertMany(listOf(metadata))
  }

  override fun insertMany(metadatas: Collection<BookMetadata>) {
    if (metadatas.isNotEmpty()) {
      dsl.transaction { config ->
        config.dsl().batch(
          config.dsl().insertInto(
            d,
            d.BOOK_ID,
            d.TITLE,
            d.TITLE_LOCK,
            d.SUMMARY,
            d.SUMMARY_LOCK,
            d.NUMBER,
            d.NUMBER_LOCK,
            d.NUMBER_SORT,
            d.NUMBER_SORT_LOCK,
            d.RELEASE_DATE,
            d.RELEASE_DATE_LOCK,
            d.AUTHORS_LOCK,
            d.TAGS_LOCK
          ).values(null as String?, null, null, null, null, null, null, null, null, null, null, null, null)
        ).also { step ->
          metadatas.forEach {
            step.bind(
              it.bookId,
              it.title,
              it.titleLock,
              it.summary,
              it.summaryLock,
              it.number,
              it.numberLock,
              it.numberSort,
              it.numberSortLock,
              it.releaseDate,
              it.releaseDateLock,
              it.authorsLock,
              it.tagsLock
            )
          }
        }.execute()

        insertAuthors(config.dsl(), metadatas)
        insertTags(config.dsl(), metadatas)
      }
    }
  }

  override fun update(metadata: BookMetadata) {
    dsl.transaction { config ->
      updateMetadata(config.dsl(), metadata)
    }
  }

  override fun updateMany(metadatas: Collection<BookMetadata>) {
    dsl.transaction { config ->
      metadatas.forEach { updateMetadata(config.dsl(), it) }
    }
  }

  private fun updateMetadata(dsl: DSLContext, metadata: BookMetadata) {
    dsl.transaction { config ->
      with(config.dsl()) {
        update(d)
          .set(d.TITLE, metadata.title)
          .set(d.TITLE_LOCK, metadata.titleLock)
          .set(d.SUMMARY, metadata.summary)
          .set(d.SUMMARY_LOCK, metadata.summaryLock)
          .set(d.NUMBER, metadata.number)
          .set(d.NUMBER_LOCK, metadata.numberLock)
          .set(d.NUMBER_SORT, metadata.numberSort)
          .set(d.NUMBER_SORT_LOCK, metadata.numberSortLock)
          .set(d.RELEASE_DATE, metadata.releaseDate)
          .set(d.RELEASE_DATE_LOCK, metadata.releaseDateLock)
          .set(d.AUTHORS_LOCK, metadata.authorsLock)
          .set(d.TAGS_LOCK, metadata.tagsLock)
          .set(d.LAST_MODIFIED_DATE, LocalDateTime.now(ZoneId.of("Z")))
          .where(d.BOOK_ID.eq(metadata.bookId))
          .execute()

        deleteFrom(a)
          .where(a.BOOK_ID.eq(metadata.bookId))
          .execute()
        deleteFrom(bt)
          .where(bt.BOOK_ID.eq(metadata.bookId))
          .execute()

        insertAuthors(this, listOf(metadata))
        insertTags(config.dsl(), listOf(metadata))
      }
    }
  }

  private fun insertAuthors(dsl: DSLContext, metadatas: Collection<BookMetadata>) {
    if (metadatas.any { it.authors.isNotEmpty() }) {
      dsl.batch(
        dsl.insertInto(a, a.BOOK_ID, a.NAME, a.ROLE)
          .values(null as String?, null, null)
      ).also { step ->
        metadatas.forEach { metadata ->
          metadata.authors.forEach {
            step.bind(metadata.bookId, it.name, it.role)
          }
        }
      }.execute()
    }
  }

  private fun insertTags(dsl: DSLContext, metadatas: Collection<BookMetadata>) {
    if (metadatas.any { it.tags.isNotEmpty() }) {
      dsl.batch(
        dsl.insertInto(bt, bt.BOOK_ID, bt.TAG)
          .values(null as String?, null)
      ).also { step ->
        metadatas.forEach { metadata ->
          metadata.tags.forEach {
            step.bind(metadata.bookId, it)
          }
        }
      }.execute()
    }
  }

  override fun delete(bookId: String) {
    dsl.transaction { config ->
      with(config.dsl()) {
        deleteFrom(a).where(a.BOOK_ID.eq(bookId)).execute()
        deleteFrom(bt).where(bt.BOOK_ID.eq(bookId)).execute()
        deleteFrom(d).where(d.BOOK_ID.eq(bookId)).execute()
      }
    }
  }

  override fun deleteByBookIds(bookIds: Collection<String>) {
    dsl.transaction { config ->
      with(config.dsl()) {
        deleteFrom(a).where(a.BOOK_ID.`in`(bookIds)).execute()
        deleteFrom(bt).where(bt.BOOK_ID.`in`(bookIds)).execute()
        deleteFrom(d).where(d.BOOK_ID.`in`(bookIds)).execute()
      }
    }
  }

  private fun BookMetadataRecord.toDomain(authors: List<Author>, tags: Set<String>) =
    BookMetadata(
      title = title,
      summary = summary,
      number = number,
      numberSort = numberSort,
      releaseDate = releaseDate,
      authors = authors,
      tags = tags,

      bookId = bookId,

      createdDate = createdDate.toCurrentTimeZone(),
      lastModifiedDate = lastModifiedDate.toCurrentTimeZone(),

      titleLock = titleLock,
      summaryLock = summaryLock,
      numberLock = numberLock,
      numberSortLock = numberSortLock,
      releaseDateLock = releaseDateLock,
      authorsLock = authorsLock,
      tagsLock = tagsLock
    )

  private fun BookMetadataAuthorRecord.toDomain() =
    Author(
      name = name,
      role = role
    )
}
