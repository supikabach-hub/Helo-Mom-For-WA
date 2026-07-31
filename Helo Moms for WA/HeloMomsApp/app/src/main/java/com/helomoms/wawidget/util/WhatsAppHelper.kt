package com.helomoms.wawidget.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.helomoms.wawidget.data.ActionType
import com.helomoms.wawidget.data.ContactItem

object WhatsAppHelper {

    fun executeAction(context: Context, contactItem: ContactItem) {
        val cleanPhone = contactItem.phoneNumber
        if (cleanPhone.isEmpty()) {
            Toast.makeText(context, "Nomor WhatsApp kontak kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            when (contactItem.actionType) {
                ActionType.VIDEO_CALL -> {
                    // Try launching video call, fallback to direct chat if contact not in address book
                    launchWhatsAppDirect(context, cleanPhone, isVideo = true)
                }
                ActionType.VOICE_CALL -> {
                    launchWhatsAppDirect(context, cleanPhone, isVideo = false)
                }
                ActionType.CHAT -> {
                    launchWhatsAppChat(context, cleanPhone)
                }
            }
        } catch (e: Exception) {
            // Fallback to wa.me link
            launchWhatsAppChat(context, cleanPhone)
        }
    }

    private fun launchWhatsAppDirect(context: Context, phone: String, isVideo: Boolean) {
        try {
            // First try direct URI
            val actionUri = Uri.parse("https://wa.me/$phone")
            val intent = Intent(Intent.ACTION_VIEW, actionUri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            launchWhatsAppChat(context, phone)
        }
    }

    private fun launchWhatsAppChat(context: Context, phone: String) {
        try {
            val uri = Uri.parse("https://wa.me/$phone")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Aplikasi WhatsApp belum terpasang!", Toast.LENGTH_LONG).show()
        }
    }
}
