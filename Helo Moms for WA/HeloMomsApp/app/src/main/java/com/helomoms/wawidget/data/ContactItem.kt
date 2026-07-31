package com.helomoms.wawidget.data

enum class ActionType(val labelString: String) {
    VIDEO_CALL("Panggilan Video WA"),
    VOICE_CALL("Panggilan Suara WA"),
    CHAT("Kirim Pesan / Chat")
}

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String, // e.g. 628123456789
    val photoUriString: String,
    val actionType: ActionType = ActionType.VIDEO_CALL,
    val pageOrder: Int = 0
)
