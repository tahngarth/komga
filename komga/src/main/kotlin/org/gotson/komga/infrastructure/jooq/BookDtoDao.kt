package org.gotson.komga.infrastructure.jooq

import org.gotson.komga.interfaces.rest.dto.AuthorDto
import org.gotson.komga.interfaces.rest.dto.BookDto
import org.gotson.komga.interfaces.rest.dto.BookMetadataDto
import org.gotson.komga.interfaces.rest.dto.MediaDto
import org.gotson.komga.interfaces.rest.persistence.BookDtoRepository
import org.gotson.komga.interfaces.rest.persistence.BookSearch
import org.gotson.komga.jooq.Tables
import org.gotson.komga.jooq.tables.records.BookMetadataAuthorRecord
import org.gotson.komga.jooq.tables.records.BookMetadataRecord
import org.gotson.komga.jooq.tables.records.BookRecord
import org.gotson.komga.jooq.tables.records.MediaRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.ResultQuery
import org.jooq.impl.DSL
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.net.URL

@Component
class BookDtoDao(
  private val dsl: DSLContext
) : BookDtoRepository {

  private val b = Tables.BOOK
  private val m = Tables.MEDIA
  private val p = Tables.MEDIA_PAGE
  private val d = Tables.BOOK_METADATA
  private val a = Tables.BOOK_METADATA_AUTHOR

  private val mediaFields = b.media().fields().filterNot { it.name == b.media().THUMBNAIL.name }.toTypedArray()
  private val groupFields = arrayOf(
    *b.fields(),
    *mediaFields,
    *b.bookMetadata().fields(),
    *a.fields()
  )

  private val sorts = mapOf(
    "metadata.numberSort" to b.bookMetadata().NUMBER_SORT,
    "createdDate" to b.CREATED_DATE,
    "lastModifiedDate" to b.LAST_MODIFIED_DATE,
    "fileSize" to b.FILE_SIZE
  )

  override fun findAll(search: BookSearch, pageable: Pageable): Page<BookDto> {
    val conditions = search.toCondition()

    val count = dsl.selectCount()
      .from(b)
      .join(m).on(b.MEDIA_ID.eq(m.ID))
      .join(d).on(b.METADATA_ID.eq(d.ID))
      .where(conditions)
      .fetchOne(0, Int::class.java)

    val orderBy = pageable.toOrderBy(sorts)

    val dtos = selectBase()
      .where(conditions)
      .groupBy(*groupFields)
      .orderBy(orderBy)
      .limit(pageable.pageSize)
      .offset(pageable.offset)
      .fetchAndMap()

    return PageImpl(
      dtos,
      PageRequest.of(pageable.pageNumber, pageable.pageSize, pageable.sort),
      count.toLong()
    )
  }

  override fun findByIdOrNull(bookId: Long): BookDto? =
    selectBase()
      .where(b.ID.eq(bookId))
      .groupBy(*groupFields)
      .fetchAndMap()
      .firstOrNull()

  override fun getLibraryId(bookId: Long): Long? =
    dsl.select(b.LIBRARY_ID)
      .from(b)
      .where(b.ID.eq(bookId))
      .fetchOne(0, Long::class.java)

  override fun getThumbnail(bookId: Long): ByteArray? =
    dsl.select(b.media().THUMBNAIL)
      .from(b)
      .where(b.ID.eq(bookId))
      .fetchOne(0, ByteArray::class.java)

  override fun findPreviousInSeries(bookId: Long): BookDto? = findSibling(bookId, next = false)

  override fun findNextInSeries(bookId: Long): BookDto? = findSibling(bookId, next = true)

  override fun findFirstIdInSeries(seriesId: Long): Long? =
    dsl.select(b.ID)
      .from(b)
      .where(b.SERIES_ID.eq(seriesId))
      .orderBy(b.bookMetadata().NUMBER_SORT)
      .limit(1)
      .fetchOne(0, Long::class.java)

  override fun findAllIdBySeriesId(seriesId: Long): Collection<Long> =
    dsl.select(b.ID)
      .from(b)
      .where(b.SERIES_ID.eq(seriesId))
      .fetch(0, Long::class.java)

  override fun findAllIdByLibraryId(libraryId: Long): Collection<Long> =
    dsl.select(b.ID)
      .from(b)
      .where(b.LIBRARY_ID.eq(libraryId))
      .fetch(0, Long::class.java)

  override fun findAllId(bookSearch: BookSearch): Collection<Long> {
    val conditions = bookSearch.toCondition()

    return dsl.select(b.ID)
      .from(b)
      .where(conditions)
      .fetch(0, Long::class.java)
  }


  private fun findSibling(bookId: Long, next: Boolean): BookDto? {
    val record = dsl.select(b.SERIES_ID, b.bookMetadata().NUMBER_SORT)
      .from(b)
      .where(b.ID.eq(bookId))
      .fetchOne()
    val seriesId = record.get(0, Long::class.java)
    val numberSort = record.get(1, Float::class.java)

    return selectBase()
      .where(b.SERIES_ID.eq(seriesId))
      .groupBy(*groupFields)
      .orderBy(b.bookMetadata().NUMBER_SORT.let { if (next) it.asc() else it.desc() })
      .seek(numberSort)
      .limit(1)
      .fetchAndMap()
      .firstOrNull()
  }

  private fun selectBase() =
    dsl.select(*groupFields)
      .select(DSL.count(p.NUMBER).`as`("pageCount"))
      .from(b)
      .leftJoin(p).on(b.media().ID.eq(p.MEDIA_ID))
      .leftJoin(a).on(b.bookMetadata().ID.eq(a.BOOK_METADATA_ID))

  private fun ResultQuery<Record>.fetchAndMap() =
    fetchGroups(
      { it.into(*b.fields(), *mediaFields, *b.bookMetadata().fields(), DSL.field("pageCount")) }, { it.into(a) }
    ).map { (r, ar) ->
      val br = r.into(b)
      val mr = r.into(b.media())
      val dr = r.into(b.bookMetadata())
      val pageCount = r["pageCount"] as Int
      br.toDto(mr.toDto(pageCount), dr.toDto(ar))
    }

  private fun BookSearch.toCondition(): Condition {
    var c: Condition = DSL.trueCondition()

    if (libraryIds.isNotEmpty()) c = c.and(b.LIBRARY_ID.`in`(libraryIds))
    if (seriesIds.isNotEmpty()) c = c.and(b.SERIES_ID.`in`(seriesIds))
    searchTerm?.let { c = c.and(b.bookMetadata().TITLE.containsIgnoreCase(it)) }
    if (mediaStatus.isNotEmpty()) c = c.and(b.media().STATUS.`in`(mediaStatus))

    return c
  }

  private fun BookRecord.toDto(media: MediaDto, metadata: BookMetadataDto) =
    BookDto(
      id = id,
      seriesId = seriesId,
      libraryId = libraryId,
      name = name,
      url = URL(url).toURI().path,
      number = number,
      created = createdDate?.toUTC(),
      lastModified = lastModifiedDate?.toUTC(),
      fileLastModified = fileLastModified.toUTC(),
      sizeBytes = fileSize,
      media = media,
      metadata = metadata
    )

  private fun MediaRecord.toDto(pageCount: Int) =
    MediaDto(
      status = status,
      mediaType = mediaType ?: "",
      pagesCount = pageCount,
      comment = comment ?: ""
    )

  private fun BookMetadataRecord.toDto(ar: Collection<BookMetadataAuthorRecord>) =
    BookMetadataDto(
      title = title,
      titleLock = titleLock,
      summary = summary,
      summaryLock = summaryLock,
      number = number,
      numberLock = numberLock,
      numberSort = numberSort,
      numberSortLock = numberSortLock,
      readingDirection = readingDirection ?: "",
      readingDirectionLock = readingDirectionLock,
      publisher = publisher,
      publisherLock = publisherLock,
      ageRating = ageRating,
      ageRatingLock = ageRatingLock,
      releaseDate = releaseDate,
      releaseDateLock = releaseDateLock,
      authors = ar.filter { it.name != null }.map { AuthorDto(it.name, it.role) },
      authorsLock = authorsLock
    )
}
