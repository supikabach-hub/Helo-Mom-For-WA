package com.helomoms.wawidget.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.helomoms.wawidget.widget.AlbumWidgetProvider
import java.util.UUID

class ContactRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "helo_moms_contacts_prefs"
        private const val KEY_CONTACT_LIST = "key_contact_list"
        private const val KEY_CURRENT_PAGE = "key_current_page_index"
    }

    fun getAllContacts(): List<ContactItem> {
        val json = prefs.getString(KEY_CONTACT_LIST, null)
        if (json.isNullOrEmpty()) {
            return getDefaultSampleContacts()
        }
        return try {
            val type = object : TypeToken<List<ContactItem>>() {}.type
            val list: List<ContactItem> = gson.fromJson(json, type)
            list.sortedBy { it.pageOrder }
        } catch (e: Exception) {
            getDefaultSampleContacts()
        }
    }

    fun saveAllContacts(contacts: List<ContactItem>) {
        val sorted = contacts.mapIndexed { idx, item ->
            item.copy(pageOrder = idx)
        }
        val json = gson.toJson(sorted)
        prefs.edit().putString(KEY_CONTACT_LIST, json).apply()
        notifyWidgetUpdate()
    }

    fun addContact(name: String, phoneNumber: String, photoUriString: String, actionType: ActionType) {
        val current = getAllContacts().toMutableList()
        val cleanPhone = cleanPhoneNumber(phoneNumber)
        val newItem = ContactItem(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phoneNumber = cleanPhone,
            photoUriString = photoUriString,
            actionType = actionType,
            pageOrder = current.size
        )
        current.add(newItem)
        saveAllContacts(current)
    }

    fun deleteContact(id: String) {
        val current = getAllContacts().filter { it.id != id }
        saveAllContacts(current)
    }

    fun moveContactUp(id: String) {
        val list = getAllContacts().toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            saveAllContacts(list)
        }
    }

    fun moveContactDown(id: String) {
        val list = getAllContacts().toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            saveAllContacts(list)
        }
    }

    fun getCurrentPageIndex(): Int {
        val total = getAllContacts().size
        if (total == 0) return 0
        var idx = prefs.getInt(KEY_CURRENT_PAGE, 0)
        if (idx >= total || idx < 0) {
            idx = 0
            prefs.edit().putInt(KEY_CURRENT_PAGE, idx).apply()
        }
        return idx
    }

    fun nextPage(): Int {
        val total = getAllContacts().size
        if (total <= 1) return 0
        val next = (getCurrentPageIndex() + 1) % total
        prefs.edit().putInt(KEY_CURRENT_PAGE, next).apply()
        notifyWidgetUpdate()
        return next
    }

    fun prevPage(): Int {
        val total = getAllContacts().size
        if (total <= 1) return 0
        val prev = if (getCurrentPageIndex() - 1 < 0) total - 1 else getCurrentPageIndex() - 1
        prefs.edit().putInt(KEY_CURRENT_PAGE, prev).apply()
        notifyWidgetUpdate()
        return prev
    }

    fun getCurrentContact(): ContactItem? {
        val list = getAllContacts()
        if (list.isEmpty()) return null
        val idx = getCurrentPageIndex()
        return list.getOrNull(idx) ?: list.first()
    }

    fun notifyWidgetUpdate() {
        val intent = Intent(context, AlbumWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, AlbumWidgetProvider::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    private fun cleanPhoneNumber(phone: String): String {
        var clean = phone.replace(Regex("[^0-9]"), "")
        if (clean.startsWith("0")) {
            clean = "62" + clean.substring(1)
        } else if (clean.startsWith("+62")) {
            clean = "62" + clean.substring(3)
        }
        return clean
    }

    private fun getDefaultSampleContacts(): List<ContactItem> {
        val sample = listOf(
            ContactItem(
                id = "sample-1",
                name = "Anak Ke-1 (Budi)",
                phoneNumber = "628123456789",
                photoUriString = "",
                actionType = ActionType.VIDEO_CALL,
                pageOrder = 0
            ),
            ContactItem(
                id = "sample-2",
                name = "Cucu (Siti)",
                phoneNumber = "628987654321",
                photoUriString = "",
                actionType = ActionType.VIDEO_CALL,
                pageOrder = 1
            )
        )
        // save default initially
        val json = gson.toJson(sample)
        prefs.edit().putString(KEY_CONTACT_LIST, json).apply()
        return sample
    }
}
