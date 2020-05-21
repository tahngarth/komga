package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.Series
import java.net.URL

interface SeriesRepository {
  fun findAll(): Collection<Series>
  fun findByIdOrNull(seriesId: Long): Series?
  fun findByLibraryId(libraryId: Long): Collection<Series>
  fun findByLibraryIdAndUrlNotIn(libraryId: Long, urls: Collection<URL>): Collection<Series>
  fun findByLibraryIdAndUrl(libraryId: Long, url: URL): Series?

  fun insert(series: Series): Series
  fun update(series: Series)

  fun delete(seriesId: Long)
  fun deleteAll()
  fun deleteAll(seriesIds: Collection<Long>)

  fun count(): Long
}
