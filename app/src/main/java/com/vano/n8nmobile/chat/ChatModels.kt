package com.vano.n8nmobile.chat

data class ChatMessage(
    val role: String,
    val text: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = null,
    val attachmentName: String? = null
)
