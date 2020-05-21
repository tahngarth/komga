package org.gotson.komga.domain.model

class Media(
  var status: Status = Status.UNKNOWN,

  var mediaType: String? = null,

  var thumbnail: ByteArray? = null,

  pages: Iterable<BookPage> = emptyList(),

  files: Iterable<String> = emptyList(),

  var comment: String? = null
) : AuditableEntity() {
  var bookId: Long = 0

  private var _pages: MutableList<BookPage> = mutableListOf()

  var pages: List<BookPage>
    get() = _pages.toList()
    set(value) {
      _pages.clear()
      _pages.addAll(value)
    }

  private var _files: MutableList<String> = mutableListOf()

  var files: List<String>
    get() = _files.toList()
    set(value) {
      _files.clear()
      _files.addAll(value)
    }

  fun reset() {
    status = Status.UNKNOWN
    mediaType = null
    thumbnail = null
    comment = null
    _pages.clear()
    _files.clear()
  }

  init {
    this.pages = pages.toList()
    this.files = files.toList()
  }

  enum class Status {
    UNKNOWN, ERROR, READY, UNSUPPORTED
  }

}
