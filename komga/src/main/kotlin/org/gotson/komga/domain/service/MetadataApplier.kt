package org.gotson.komga.domain.service

import mu.KotlinLogging
import org.gotson.komga.domain.model.BookMetadata
import org.gotson.komga.domain.model.BookMetadataPatch
import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.model.SeriesMetadataPatch
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class MetadataApplier {

  fun apply(patch: BookMetadataPatch, metadata: BookMetadata) {
    logger.debug { "Apply metadata for book: ${metadata.bookId}" }

    with(metadata) {
      patch.title?.let {
        if (!titleLock) {
          logger.debug { "Update title: $it" }
          title = it
        } else
          logger.debug { "title is locked, skipping" }
      }


      patch.summary?.let {
        if (!summaryLock) {
          logger.debug { "Update summary: $it" }
          summary = it
        } else
          logger.debug { "summary is locked, skipping" }
      }

      patch.number?.let {
        if (!numberLock) {
          logger.debug { "Update number: $it" }
          number = it
        } else
          logger.debug { "number is locked, skipping" }
      }

      patch.numberSort?.let {
        if (!numberSortLock) {
          logger.debug { "Update numberSort: $it" }
          numberSort = it
        } else
          logger.debug { "numberSort is locked, skipping" }
      }

      patch.readingDirection?.let {
        if (!readingDirectionLock) {
          logger.debug { "Update readingDirection: $it" }
          readingDirection = it
        } else
          logger.debug { "readingDirection is locked, skipping" }
      }

      patch.releaseDate?.let {
        if (!releaseDateLock) {
          logger.debug { "Update releaseDate: $it" }
          releaseDate = it
        } else
          logger.debug { "releaseDate is locked, skipping" }
      }

      patch.ageRating?.let {
        if (!ageRatingLock) {
          logger.debug { "Update ageRating: $it" }
          ageRating = it
        } else
          logger.debug { "ageRating is locked, skipping" }
      }

      patch.publisher?.let {
        if (!publisherLock) {
          logger.debug { "Update publisher: $it" }
          publisher = it
        } else
          logger.debug { "publisher is locked, skipping" }
      }

      patch.authors?.let {
        if (!authorsLock) {
          logger.debug { "Update authors: $it" }
          authors = it.toMutableList()
        } else
          logger.debug { "authors is locked, skipping" }
      }
    }
  }

  fun apply(patch: SeriesMetadataPatch, seriesMetadata: SeriesMetadata) {
    with(seriesMetadata) {
      patch.title?.let {
        if (!titleLock) {
          logger.debug { "Update title: $it" }
          title = it
        } else
          logger.debug { "title is locked, skipping" }
      }

      patch.titleSort?.let {
        if (!titleSortLock) {
          logger.debug { "Update titleSort: $it" }
          titleSort = it
        } else
          logger.debug { "titleSort is locked, skipping" }
      }

      patch.status?.let {
        if (!statusLock) {
          logger.debug { "status number: $it" }
          status = it
        } else
          logger.debug { "status is locked, skipping" }
      }
    }
  }

}
