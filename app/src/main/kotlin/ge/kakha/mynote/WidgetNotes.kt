package ge.kakha.mynote

import android.content.Context
import android.graphics.Color

object WidgetNotes {
    // ვიჯეტისთვის აქტიური ნოუტები: არქივი და წაშლილი არ ჩანს.
    // სტილი იგივეა, რაც აპში: ფერმკრთალი კატეგორიის ფერი + კატეგორიის ზოლი.
    fun load(context: Context): List<Note> {
        val out = mutableListOf<Note>()
        try {
            val p = context.getSharedPreferences("mynote", Context.MODE_PRIVATE)
            val saved = p.getString("notes", "") ?: ""
            for (l in saved.lines()) {
                if (l.isBlank()) continue
                try {
                    val a = l.split("|")
                    if (a.size >= 6) {
                        val type = if (a.size >= 7) a[6] else "text"
                        val list = mutableListOf<CheckItem>()
                        if (a.size >= 8 && a[7].isNotEmpty()) {
                            for (part in a[7].split(";;")) {
                                val b = part.split("~")
                                if (b.size >= 2) list.add(CheckItem(decSafe(b[1]), b[0] == "1"))
                            }
                        }
                        val id = a[0].toIntOrNull() ?: continue
                        val archived = if (a.size >= 11) a[10].toBoolean() else false
                        val deleted = if (a.size >= 12) a[11].toBoolean() else false
                        if (archived || deleted) continue
                        out.add(
                            Note(
                                id,
                                decSafe(a[1]),
                                decSafe(a[2]),
                                decSafe(a[3]),
                                a[4].toBoolean(),
                                decSafe(a[5]),
                                type,
                                list,
                                if (a.size >= 9) a[8].toLongOrNull() ?: 0L else 0L,
                                if (a.size >= 10) a[9].toLongOrNull() ?: 0L else 0L,
                                archived,
                                deleted
                            )
                        )
                    }
                } catch (e: Exception) { /* დაზიანებული ხაზი გამოტოვებულია */ }
            }
        } catch (e: Exception) { }
        out.sortWith(compareByDescending<Note> { it.isFav }.thenByDescending { it.id })
        return out
    }

    fun lockedIds(context: Context): Set<String> {
        return context.getSharedPreferences("mynote", Context.MODE_PRIVATE)
            .getStringSet("lockedNotes", emptySet()) ?: emptySet()
    }

    // კატეგორიის ფერი — იგივე, რაც HomePage-ში (catColor_<კატეგორია>)
    fun catColor(context: Context, cat: String): Int {
        val p = context.getSharedPreferences("mynote", Context.MODE_PRIVATE)
        val hex = p.getString("catColor_" + cat, "#B9AFFF") ?: "#B9AFFF"
        return try { Color.parseColor(hex) } catch (e: Exception) { Color.parseColor("#B9AFFF") }
    }

    // ფერმკრთალი ფერი — HomePage-ის paleColor: 7% ფერი + 93% თეთრი
    fun paleColor(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.rgb(
            (r * 0.07f + 255 * 0.93f).toInt(),
            (g * 0.07f + 255 * 0.93f).toInt(),
            (b * 0.07f + 255 * 0.93f).toInt()
        )
    }

    // კატეგორიის სახელი — იგივე თარგმანით, რაც HomePage-ში
    fun folderName(context: Context, folder: String): String = when (folder) {
        "ყველა" -> Lang.strCtx(context, "all")
        "პირადი" -> Lang.strCtx(context, "personal")
        else -> folder
    }

    // საჩვენებელი ტექსტი — იგივე, რაც HomePage-ში:
    // დაბლოკილი -> ••••••••, ჩექლისტი -> „ჩექლისტი • შესრულებული/სულ", ჩვეულებრივი -> ტექსტი
    fun preview(context: Context, n: Note): String {
        if (lockedIds(context).contains(n.id.toString())) return "••••••••"
        if (n.noteType == "checklist") {
            val total = n.checklist.size
            val done = n.checklist.count { it.done }
            return Lang.strCtx(context, "checklist") + " • " + done + "/" + total
        }
        return n.text
    }

    // MainActivity-ის decFieldSafe-ის ანალოგი — ვიჯეტს MainActivity არ აქვს
    private fun decSafe(s: String): String { return if (isB64(s)) dec(s) else s }
    private fun dec(s: String): String {
        return try { String(android.util.Base64.decode(s, android.util.Base64.NO_WRAP), Charsets.UTF_8) } catch (e: Exception) { s }
    }
    private fun isB64(s: String): Boolean {
        if (s.isEmpty()) return true
        if (s.length % 4 != 0) return false
        return s.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' }
    }
}
