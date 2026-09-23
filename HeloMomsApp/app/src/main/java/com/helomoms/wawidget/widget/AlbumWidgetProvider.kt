package com.helomoms.wawidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.helomoms.wawidget.R
import com.helomoms.wawidget.data.ActionType
import com.helomoms.wawidget.data.ContactRepository
import com.helomoms.wawidget.util.WhatsAppHelper

class AlbumWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_PAGE = "com.helomoms.wawidget.ACTION_NEXT_PAGE"
        const val ACTION_PREV_PAGE = "com.helomoms.wawidget.ACTION_PREV_PAGE"
        const val ACTION_CALL_WHATSAPP = "com.helomoms.wawidget.ACTION_CALL_WHATSAPP"
        const val EXTRA_CONTACT_PHONE = "extra_contact_phone"
        const val EXTRA_ACTION_TYPE = "extra_action_type"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val repository = ContactRepository(context)

        when (intent.action) {
            ACTION_NEXT_PAGE -> {
                repository.nextPage()
                refreshAllWidgets(context)
            }
            ACTION_PREV_PAGE -> {
                repository.prevPage()
                refreshAllWidgets(context)
            }
        }
    }

    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, AlbumWidgetProvider::class.java)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_album_layout)
        val repository = ContactRepository(context)
        val allContacts = repository.getAllContacts()

        if (allContacts.isEmpty()) {
            views.setTextViewText(R.id.tv_contact_name, "Belum Ada Kontak")
            views.setTextViewText(R.id.tv_page_indicator, "0 dari 0")
            // Launch MainActivity when clicking empty widget
            val launchIntent = Intent(context, com.helomoms.wawidget.ui.MainActivity::class.java)
            val pendingLaunch = PendingIntent.getActivity(
                context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_video_call, pendingLaunch)
            views.setOnClickPendingIntent(R.id.btn_chat_wa, pendingLaunch)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val currentIndex = repository.getCurrentPageIndex()
        val contact = allContacts[currentIndex]

        // Set name and page indicator
        views.setTextViewText(R.id.tv_contact_name, contact.name)
        views.setTextViewText(
            R.id.tv_page_indicator,
            "Foto ${currentIndex + 1} dari ${allContacts.size}"
        )

        // Both Video Call and Chat WA buttons will launch the WA intent
        // (Due to Android limitations, direct video call starting is restricted, so both open the chat)

        // Try loading photo if uri exists
        var photoSet = false
        if (contact.photoUriString.isNotEmpty()) {
            try {
                val uri = Uri.parse(contact.photoUriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    
                    // Scale down bitmap to prevent TransactionTooLargeException
                    val maxDim = 300
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val ratio = width.toFloat() / height.toFloat()
                    val finalWidth = if (width > height) maxDim else (maxDim * ratio).toInt()
                    val finalHeight = if (height > width) maxDim else (maxDim / ratio).toInt()
                    
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true)
                    
                    views.setImageViewBitmap(R.id.img_contact_photo, scaledBitmap)
                    photoSet = true
                    inputStream.close()
                }
            } catch (e: Exception) {
                photoSet = false
            }
        }
        if (!photoSet) {
            // Set fallback background color or icon
            views.setImageViewResource(R.id.img_contact_photo, android.R.drawable.ic_menu_camera)
        }

        // Set click listeners for pagination arrows
        views.setOnClickPendingIntent(
            R.id.btn_prev,
            getPendingIntent(context, ACTION_PREV_PAGE, 101)
        )
        views.setOnClickPendingIntent(
            R.id.btn_next,
            getPendingIntent(context, ACTION_NEXT_PAGE, 102)
        )

        // Set click listener for Call WhatsApp buttons using a direct Activity Intent
        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${contact.phoneNumber}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val waPendingIntent = PendingIntent.getActivity(
            context,
            contact.id.hashCode(),
            waIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_video_call, waPendingIntent)
        views.setOnClickPendingIntent(R.id.btn_chat_wa, waPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingIntent(context: Context, actionString: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlbumWidgetProvider::class.java).apply {
            action = actionString
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
