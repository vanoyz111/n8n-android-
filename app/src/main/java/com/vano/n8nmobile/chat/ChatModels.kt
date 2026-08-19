package com.vano.n8nmobile.chat

data class ImageAttachment(
    val base64: String,
    val mimeType: String
)

data class ChatMessage(
    val role: String,
    val text: String,
    val imageAttachments: List<ImageAttachment> = emptyList(),
    val fileAttachmentNames: List<String> = emptyList()
)
