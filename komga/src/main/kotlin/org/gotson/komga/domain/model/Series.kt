package org.gotson.komga.domain.model

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.net.URL
import java.time.LocalDateTime
import javax.persistence.Cacheable
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "series")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "cache.series")
class Series(
  @NotBlank
  @Column(name = "name", nullable = false)
  var name: String,

  @Column(name = "url", nullable = false)
  var url: URL,

  @Column(name = "file_last_modified", nullable = false)
  var fileLastModified: LocalDateTime
) : AuditableEntity() {
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, unique = true)
  var id: Long = 0

  @NotNull
  var libraryId: Long = 0L

  @OneToOne(optional = false, orphanRemoval = true, cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @JoinColumn(name = "metadata_id", nullable = false)
  var metadata: SeriesMetadata = SeriesMetadata(title = name)

  override fun toString(): String = "Series($id, ${url.toURI().path})"
}
