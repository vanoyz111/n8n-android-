package com.vano.n8nmobile.docchat

object DocumentChunker {
    private const val WORDS_PER_CHUNK = 200

    fun chunk(text: String): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return words.chunked(WORDS_PER_CHUNK).map { it.joinToString(" ") }
    }

    fun retrieveRelevantChunks(chunks: List<String>, query: String, topN: Int = 3, maxTotalChars: Int = 3500): List<String> {
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        if (chunks.isEmpty()) return emptyList()
        if (queryWords.isEmpty()) return chunks.take(topN)

        val scored = chunks.map { chunk ->
            val chunkWords = chunk.lowercase().split(Regex("\\W+"))
            chunk to chunkWords.count { it in queryWords }
        }.sortedByDescending { it.second }

        val result = mutableListOf<String>()
        var totalChars = 0
        for ((chunk, score) in scored) {
            if (result.size >= topN) break
            if (score == 0 && result.isNotEmpty()) break
            if (totalChars + chunk.length > maxTotalChars) continue
            result.add(chunk)
            totalChars += chunk.length
        }
        if (result.isEmpty()) result.add(scored.first().first)
        return result
    }
}
