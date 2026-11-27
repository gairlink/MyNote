package ge.kakha.mynote

import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object PinGate {
    fun hash(pin: String): String { val md = java.security.MessageDigest.getInstance("SHA-256"); return md.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) } }
    fun checkHash(a: MainActivity, pin: String): Boolean { val saved = a.getSharedPreferences("mynote", 0).getString("pin", null) ?: return false; return if (saved.length == 64 && saved.all { it.isDigit() || it in 'a'..'f' }) saved == hash(pin) else saved == pin }
    fun hasPin(a: MainActivity): Boolean {
        val pin = a.getSharedPreferences("mynote", 0).getString("pin", null)
        return !pin.isNullOrEmpty()
    }

    fun check(a: MainActivity, onUnlock: () -> Unit) {
        // პროგრამაში შესასვლელად პაროლი აღარ გამოიყენება — პირდაპირ ვხსნით
        onUnlock()
    }

    // საერთო პაროლის ველი — UiDialog-ის სტილში
    private fun pinField(a: MainActivity, hint: String): EditText {
        val et = EditText(a)
        et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        et.hint = hint
        et.setPadding(UiHelpers.dp(a, 4), UiHelpers.dp(a, 12), UiHelpers.dp(a, 4), UiHelpers.dp(a, 12))
        return et
    }

    private fun cancelText(a: MainActivity, onCancel: () -> Unit): TextView {
        val cancelBtn = TextView(a)
        cancelBtn.text = Lang.str(a, "cancel"); cancelBtn.textSize = 14f
        cancelBtn.gravity = Gravity.CENTER
        cancelBtn.setTextColor(android.graphics.Color.parseColor("#7A6FF0"))
        cancelBtn.setPadding(UiHelpers.dp(a, 12), UiHelpers.dp(a, 8), UiHelpers.dp(a, 12), UiHelpers.dp(a, 8))
        cancelBtn.setOnClickListener { onCancel() }
        return cancelBtn
    }

    // პროგრამაში შესვლა — ძველი showPinDialog, ახლა UiDialog-ში (შესვლაზე აღარ გამოიყენება, მაგრამ ერთ სტილშია)
    private fun showPinDialog(a: MainActivity, onUnlock: () -> Unit) {
        var unlocked = false
        UiDialog.show(a, Lang.str(a, "enterPinTitle")) { box, dialog ->
            dialog.setOnDismissListener { if (!unlocked) a.finish() }
            val et = pinField(a, Lang.str(a, "enterPin"))
            box.addView(et)
            var attempts = 0
            box.addView(UiDialog.actionButton(a, Lang.str(a, "openApp")) {
                if (attempts >= 5) { Toast.makeText(a, Lang.str(a, "tooManyAttempts"), Toast.LENGTH_SHORT).show(); return@actionButton }
                if (checkHash(a, et.text.toString().trim())) { unlocked = true; dialog.dismiss(); onUnlock() }
                else { attempts++; Toast.makeText(a, Lang.str(a, "wrongPin"), Toast.LENGTH_SHORT).show(); et.setText("") }
            })
        }
    }

    // დაბლოკვისთვის: თუ პაროლი არ არის, ჯერ დააყენებინე, თუ არის — მაშინვე მზადაა
    fun ensurePin(a: MainActivity, onReady: () -> Unit) {
        if (hasPin(a)) { onReady(); return }
        showSetPinDialog(a, onReady)
    }

    private fun showSetPinDialog(a: MainActivity, onReady: () -> Unit) {
        UiDialog.show(a, Lang.str(a, "setPinTitle")) { box, dialog ->
            val et = pinField(a, Lang.str(a, "setPinHint"))
            box.addView(et)
            box.addView(UiDialog.actionButton(a, Lang.str(a, "save")) {
                val pin = et.text.toString().trim()
                if (pin.length < 4) { Toast.makeText(a, Lang.str(a, "enterPin"), Toast.LENGTH_SHORT).show(); return@actionButton }
                a.getSharedPreferences("mynote", 0).edit().putString("pin", hash(pin)).apply()
                dialog.dismiss(); onReady()
            })
            box.addView(cancelText(a) { dialog.dismiss() })
        }
    }

    // დაბლოკილი ნოუტის გახსნა: პაროლის მოთხოვნა — UiDialog-ის ფანჯარაში
    fun promptForNote(a: MainActivity, onUnlock: () -> Unit, onCancel: () -> Unit) {
        if (!hasPin(a)) { ensurePin(a, onUnlock); return }
        var unlocked = false
        var cancelled = false
        UiDialog.show(a, Lang.str(a, "enterPinTitle")) { box, dialog ->
            dialog.setOnDismissListener {
                // Pictograms.close-ით დახურვა = გაუქმება, თუ ჯერ არ განიბლოკა
                if (!unlocked && !cancelled) onCancel()
            }
            val et = pinField(a, Lang.str(a, "enterPin"))
            box.addView(et)
            var attempts = 0
            box.addView(UiDialog.actionButton(a, Lang.str(a, "openApp")) {
                if (attempts >= 5) { Toast.makeText(a, Lang.str(a, "tooManyAttempts"), Toast.LENGTH_SHORT).show(); return@actionButton }
                if (checkHash(a, et.text.toString().trim())) { unlocked = true; dialog.dismiss(); onUnlock() }
                else { attempts++; Toast.makeText(a, Lang.str(a, "wrongPin"), Toast.LENGTH_SHORT).show(); et.setText("") }
            })
            box.addView(cancelText(a) { cancelled = true; dialog.dismiss(); onCancel() })
        }
    }
}