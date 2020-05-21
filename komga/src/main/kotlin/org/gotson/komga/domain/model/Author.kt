package org.gotson.komga.domain.model

import javax.validation.constraints.NotBlank

class Author {
  constructor(name: String, role: String) {
    this.name = name
    this.role = role
  }

  @NotBlank
  var name: String
    set(value) {
      require(value.isNotBlank()) { "name must not be blank" }
      field = value.trim()
    }

  @NotBlank
  var role: String
    set(value) {
      require(value.isNotBlank()) { "role must not be blank" }
      field = value.trim().toLowerCase()
    }

  override fun toString(): String = "Author($name, $role)"
}
