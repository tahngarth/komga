package org.gotson.komga.domain.model

import java.time.LocalDateTime

abstract class AuditableEntity {
  var createdDate: LocalDateTime? = null

  var lastModifiedDate: LocalDateTime? = null
}
