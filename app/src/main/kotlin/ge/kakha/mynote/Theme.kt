package ge.kakha.mynote

import android.graphics.Color

object Theme {
    fun isDark(a: MainActivity): Boolean {
        return a.getSharedPreferences("mynote", 0).getBoolean("darkMode", false)
    }
    fun setDark(a: MainActivity, dark: Boolean) {
        a.getSharedPreferences("mynote", 0).edit().putBoolean("darkMode", dark).apply()
    }
    // ფონები
    fun bg(a: MainActivity): String = if (isDark(a)) "#17171F" else a.LAV_BG
    fun paper(a: MainActivity): String = if (isDark(a)) "#23232F" else "#FFFFFF"
    fun pill(a: MainActivity): String = if (isDark(a)) "#3D3D5C" else "#D8D0FF"
    fun pillDark(a: MainActivity): String = if (isDark(a)) "#8B7FE8" else "#7A6FD0"
    fun line(a: MainActivity): String = if (isDark(a)) "#3A3A4D" else "#C9C0E8"
    // ტექსტები
    fun text(a: MainActivity): String = if (isDark(a)) "#F2F0FF" else "#1F1A33"
    fun title(a: MainActivity): String = if (isDark(a)) "#FFFFFF" else "#3D3566"
    fun sub(a: MainActivity): String = if (isDark(a)) "#B8B2E8" else "#7A6FD0"
    fun body(a: MainActivity): String = if (isDark(a)) "#D8D4F5" else "#333333"
    fun hint(a: MainActivity): String = if (isDark(a)) "#8B87B8" else "#8B7FE8"
    fun color(a: MainActivity, hex: String): Int = Color.parseColor(hex)
}