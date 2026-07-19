package com.noter.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

/**
 * Sends the current note to a Notesnook account via its Inbox API
 * (https://help.notesnook.com/inbox-api/getting-started) — the same fixed endpoint and request
 * shape as the chrome-newtab-dashboard project's notes widget. The inbox key is per-account,
 * created from Notesnook's own Settings > Inbox screen; this app only ever POSTs to it.
 */
object NotesnookApi {
    private const val INBOX_URL = "https://inbox.notesnook.com/"

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    // Plain textarea content -> minimal HTML, since the Inbox API only accepts content.type
    // "html". Blank lines become paragraph breaks, single line breaks become <br>, so the note
    // reads the same as it did on the Notes tab.
    private fun textToHtml(text: String): String =
        text.split(Regex("\n{2,}")).joinToString(separator = "") { para ->
            "<p>${escapeHtml(para).replace("\n", "<br>")}</p>"
        }

    // The API requires a non-empty title -- it does not fill one in on its own -- so this stands
    // in for "no title" with the current local date and time.
    private fun defaultNoteTitle(): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())

    suspend fun sendNote(apiKey: String, text: String, tagId: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("title", defaultNoteTitle())
                put("type", "note")
                put("source", "noter-android")
                put("version", 1)
                put("content", JSONObject().apply {
                    put("type", "html")
                    put("data", textToHtml(text))
                })
                if (!tagId.isNullOrBlank()) put("tagIds", JSONArray().put(tagId))
            }

            var connection: HttpURLConnection? = null
            try {
                connection = (URL(INBOX_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", apiKey)
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                when (val status = connection.responseCode) {
                    in 200..299 -> Result.success(Unit)
                    401, 403 -> Result.failure(IOException("Notesnook rejected the API key. Check it in settings."))
                    else -> Result.failure(IOException("Notesnook returned $status."))
                }
            } catch (e: IOException) {
                Result.failure(IOException("Could not reach Notesnook's inbox service.", e))
            } finally {
                connection?.disconnect()
            }
        }
}
