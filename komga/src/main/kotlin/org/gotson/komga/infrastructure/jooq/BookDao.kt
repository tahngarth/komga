package org.gotson.komga.infrastructure.jooq

import org.apache.commons.io.FilenameUtils
import org.gotson.komga.interfaces.rest.dto.AuthorDto
import org.gotson.komga.interfaces.rest.dto.BookDto
import org.gotson.komga.interfaces.rest.dto.BookMetadataDto
import org.gotson.komga.interfaces.rest.dto.MediaDto
import org.gotson.komga.interfaces.rest.persistence.BookDtoRepository
import org.gotson.komga.interfaces.rest.persistence.BookSearch
import org.gotson.komga.jooq.Tables
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.DSL.lower
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Component
class BookDao(
  private val dsl: DSLContext
) : BookDtoRepository {

  private val b = Tables.BOOK.`as`("b")
  private val s = Tables.SERIES.`as`("s")
  private val m = Tables.MEDIA.`as`("m")
  private val p = Tables.MEDIA_PAGE.`as`("p")
  private val d = Tables.BOOK_METADATA.`as`("d")
  private val a = Tables.BOOK_METADATA_AUTHOR.`as`("a")

  private fun <T : Record> Table<T>.aliasedFields() =
    fields().map {
      it.`as`("${this.name}_${it.name}")
    }.toTypedArray()

  override fun findAll(search: BookSearch, pageable: Pageable): Page<BookDto> {
    val conditions = search.toCondition()

    val groupFields = arrayOf(
      *b.fields(),
      m.STATUS, m.MEDIA_TYPE, m.COMMENT,
      *d.fields(),
      *a.fields()
    )

    val count = dsl.selectCount()
      .from(b)
      .join(s).on(b.SERIES_ID.eq(s.ID))
      .join(m).on(b.MEDIA_ID.eq(m.ID))
      .join(d).on(b.METADATA_ID.eq(d.ID))
      .where(conditions)
      .fetchOne(0, Int::class.java)

    val dtos = dsl.select(*groupFields)
      .select(DSL.count().`as`("pageCount"))
      .from(b)
      .join(s).on(b.SERIES_ID.eq(s.ID))
      .join(m).on(b.MEDIA_ID.eq(m.ID))
      .leftJoin(p).on(m.ID.eq(p.MEDIA_ID))
      .join(d).on(b.METADATA_ID.eq(d.ID))
      .leftJoin(a).on(d.ID.eq(a.BOOK_METADATA_ID))
      .where(conditions)
      .groupBy(*groupFields)
      .orderBy(lower(d.TITLE))
      .limit(pageable.pageSize)
      .offset(pageable.offset)
      .fetchGroups(
        { it.into(*b.fields(), m.STATUS, m.MEDIA_TYPE, m.COMMENT, *d.fields(), DSL.field("pageCount")) }, { it.into(a) }
      ).map { (r, ar) ->
        val br = r.into(b)
        val mr = r.into(m)
        val dr = r.into(d)
        BookDto(
          id = br.id,
          seriesId = br.seriesId,
          name = br.name,
          url = br.url.getUrl(search.includeFullUrl),
          number = br.number,
          created = br.createdDate?.toUTC(),
          lastModified = br.lastModifiedDate?.toUTC(),
          fileLastModified = br.fileLastModified.toUTC(),
          sizeBytes = br.fileSize,
          media = MediaDto(
            status = mr.status,
            mediaType = mr.mediaType,
            pagesCount = r["pageCount"] as Int,
            comment = mr.comment ?: ""
          ),
          metadata = BookMetadataDto(
            title = dr.title,
            titleLock = dr.titleLock,
            summary = dr.summary,
            summaryLock = dr.summaryLock,
            number = dr.number,
            numberLock = dr.numberLock,
            numberSort = dr.numberSort,
            numberSortLock = dr.numberSortLock,
            readingDirection = dr.readingDirection ?: "",
            readingDirectionLock = dr.readingDirectionLock,
            publisher = dr.publisher,
            publisherLock = dr.publisherLock,
            ageRating = dr.ageRating,
            ageRatingLock = dr.ageRatingLock,
            releaseDate = dr.releaseDate,
            releaseDateLock = dr.releaseDateLock,
            authors = ar.filter { it.name != null }.map { AuthorDto(it.name, it.role) },
            authorsLock = dr.authorsLock
          )
        )
      }

    return PageImpl(
      dtos,
      PageRequest.of(pageable.pageNumber, pageable.pageSize, pageable.sort),
      count.toLong()
    )
  }

  private fun BookSearch.toCondition(): Condition {
    var c: Condition = DSL.trueCondition()

    if (libraryIds.isNotEmpty()) c = c.and(s.LIBRARY_ID.`in`(libraryIds))
    searchTerm?.let { c = c.and(d.TITLE.containsIgnoreCase(it)) }
    if (mediaStatus.isNotEmpty()) c = c.and(m.STATUS.`in`(mediaStatus))

    return c
  }

  private fun String.getUrl(fullUrl: Boolean): String {
    val path = URI(this).path
    return if (fullUrl) path else FilenameUtils.getName(path)
  }

  fun LocalDateTime.toUTC(): LocalDateTime =
    atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()

}
