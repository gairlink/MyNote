package ge.kakha.mynote

import android.content.Context

object WidgetNotes {
    fun load(context: Context): List<Note> {
        val out = mutableListOf<Note>()
        try {
            val p = context.getSharedPreferences("mynote", Context.MODE_PRIVATE)
            val saved = p.getString("notes", "") ?: ""
            for (l in saved.lines()) {
                val a = l.split("|")
                if (a.size >= 6) {
                    val type = if (a.size >= 7) a[6] else "text"
                    val list = mutableListOf<CheckItem>()
                    if (a.size >= 8 && a[7].isNotEmpty()) {
                        for (part in a[7].split(";;")) {
                            val b = part.split("~")
                            if (b.size >= 2) list.add(CheckItem(b[1], b[0] == "1"))
                        }
                    }
                    try { out.add(
                        Note(
                            a[0].toIntOrNull() ?: 0,
                            a[1],
                            a[2],
                            a[3],
                            a[4].toBoolean(),
                            a[5],
                            type,
                            list
                        )
                    ) } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
        }
        out.sortWith(compareByDescending<Note> { it.isFav }.thenByDescending { it.id })
        return out
    }
}