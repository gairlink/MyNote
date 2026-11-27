package ge.kakha.mynote

data class CheckItem(var text: String, var done: Boolean)

data class Note(
    var id: Int,
    var title: String,
    var text: String,
    var folder: String,
    var isFav: Boolean,
    var date: String,
    var noteType: String,
    var checklist: MutableList<CheckItem>,
    // შექმნისა და ბოლო რედაქტირების დრო (millis) — ძველ ჩანაწერებში 0, ანუ უცნობია
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,
    var isArchived: Boolean = false,
    var isDeleted: Boolean = false
)
