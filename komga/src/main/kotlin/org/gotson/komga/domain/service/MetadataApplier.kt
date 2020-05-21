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

  private fun <T> getIfNotLocked(original: T, patched: T?, lock: Boolean): T =
    if (patched != null && !lock) patched
    else original

  fun apply(patch: BookMetadataPatch, metadata: BookMetadata): BookMetadata {
    logger.debug { "Apply metadata for book: ${metadata.bookId}" }

    return with(metadata) {
      copy(
        title = getIfNotLocked(title, patch.title, titleLock),
        summary = getIfNotLocked(summary, patch.summary, summaryLock),
        number = getIfNotLocked(number, patch.number, numberLock),
        numberSort = getIfNotLocked(numberSort, patch.numberSort, numberSortLock),
        readingDirection = getIfNotLocked(readingDirection, patch.readingDirection, readingDirectionLock),
        releaseDate = getIfNotLocked(releaseDate, patch.releaseDate, releaseDateLock),
        ageRating = getIfNotLocked(ageRating, patch.ageRating, ageRatingLock),
        publisher = getIfNotLocked(publisher, patch.publisher, publisherLock),
        authors = getIfNotLocked(authors, patch.authors, authorsLock)
      )
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
