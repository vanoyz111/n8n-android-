package com.vano.n8nmobile.autoreply

import android.app.Notification
import java.util.UUID

object PendingReplyStore {
    data class PendingReply(
        val id: String,
        val sender: String,
        val originalMessage: String,
        var proposedReply: String,
        val timestamp: Long,
        val action: Notification.Action
    )

    private val _pending = mutableListOf<PendingReply>()

    fun getAll(): List<PendingReply> = _pending.toList()

    fun add(sender: String, originalMessage: String, proposedReply: String, action: Notification.Action): String {
        val id = UUID.randomUUID().toString()
        _pending.add(PendingReply(id, sender, originalMessage, proposedReply, System.currentTimeMillis(), action))
        return id
    }

    fun get(id: String): PendingReply? = _pending.firstOrNull { it.id == id }

    fun remove(id: String) {
        _pending.removeAll { it.id == id }
    }

    fun count(): Int = _pending.size
}
