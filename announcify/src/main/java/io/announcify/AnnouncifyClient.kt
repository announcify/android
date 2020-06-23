package io.announcify

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.announcify.model.Announcement
import okhttp3.*
import java.io.IOException
import java.util.*

class AnnouncifyClient(private val host: String, private val apiKey: String, private val projectId: Int, private val locale: Locale, private val listener: ResultListener) {

    companion object {
        val LOG_TAG = AnnouncifyClient::class.simpleName
    }

    fun request() {
        val request = Request.Builder()
            .get()
            .url("https://$host/projects/$projectId/active-announcement/active-message")
            .header("x-api-key", apiKey)
            .header("accept-language", toLanguageTag(locale))
            .build()

        val httpClient = OkHttpClient()
        Log.i(LOG_TAG, "Search for active announcements.")
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body()?.string()?.let { json ->
                    try {
                        val announcement = Gson().fromJson(json, Announcement::class.java)
                        Log.i(LOG_TAG, "Found active announcement $announcement.")

                        Handler(Looper.getMainLooper()).post {
                            listener.onSuccess(announcement)
                        }
                    } catch (e: JsonSyntaxException) {
                        Log.e(LOG_TAG, "Parsing announcement response failed!", e)

                        Handler(Looper.getMainLooper()).post {
                            listener.onFail(e)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(LOG_TAG, "Request announcements failed!", e)
                Handler(Looper.getMainLooper()).post {
                    listener.onFail(e)
                }
            }
        })
    }

    private fun toLanguageTag(locale: Locale): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return locale.toLanguageTag()
        }

        val language = locale.language
        val country = locale.country
        return "${language}_$country"
    }

    class Builder {

        private var host: String? = null
        private var apiKey: String? = null
        private var projectId: Int? = null
        private var locale: Locale? = null
        private var listener: ResultListener? = null

        fun host(host: String): Builder {
            this.host = host
            return this
        }

        fun apiKey(key: String): Builder {
            this.apiKey = key
            return this
        }

        fun projectId(id: Int): Builder {
            this.projectId = id
            return this
        }

        fun resultListener(listener: ResultListener): Builder {
            this.listener = listener
            return this
        }

        // TODO: read fields from meta fields by app's manifest (like facebook lib)
        fun build(): AnnouncifyClient {
            val host = host ?: throw IllegalArgumentException("Field host is required and must be set!")
            val apiKey = apiKey ?: throw IllegalArgumentException("Field `apiKey` is required and must be set!")
            val projectId = projectId ?: throw IllegalArgumentException("Field `projectId` is required and must be set!")
            val locale = locale ?: Locale.getDefault()
            val listener = listener ?: throw IllegalArgumentException("Field `resultListener` is required and must be set!")

            return AnnouncifyClient(host, apiKey, projectId, locale, listener)
        }
    }

    interface ResultListener {
        fun onSuccess(announcement: Announcement)
        fun onFail(e: Exception)
    }
}