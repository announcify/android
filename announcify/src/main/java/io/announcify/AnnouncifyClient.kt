package io.announcify

import android.content.Context
import android.content.pm.PackageManager
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
            .header("user-agent", "Android/v${Build.VERSION.RELEASE} " +
                    "Announcify/v${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}")
            .build()

        val httpClient = OkHttpClient()
        Log.i(LOG_TAG, "Search for active announcements.")
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code() == 404) {
                    Log.i(LOG_TAG, "No active announcement available.")
                    Handler(Looper.getMainLooper()).post {
                        listener.onNoMessage()
                    }
                    return
                }

                if (response.code() != 200) {
                    Log.e(LOG_TAG, "Request active announcement failed with HTTP error (${response.code()}!")
                    fail(Exception("Request active announcement failed. Status code: ${response.code()}"))
                    return
                }

                response.body()?.string()?.let { json ->
                    try {
                        val announcement = Gson().fromJson(json, Announcement::class.java)
                        Log.i(LOG_TAG, "Found active announcement $announcement.")

                        Handler(Looper.getMainLooper()).post {
                            listener.onMessage(announcement)
                        }
                    } catch (e: JsonSyntaxException) {
                        Log.e(LOG_TAG, "Parsing announcement response failed!", e)

                        fail(e)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(LOG_TAG, "Request active announcement failed!", e)
                fail(e)
            }
        })
    }

    private fun fail(exception: Exception) {
        Handler(Looper.getMainLooper()).post {
            listener.onFail(exception)
        }
    }

    private fun toLanguageTag(locale: Locale): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return locale.toLanguageTag()
        }

        val language = locale.language
        val country = locale.country
        return "${language}_$country"
    }

    class Builder(private val context: Context) {

        companion object {
            const val DEFAULT_HOST = "api.announcify.io"
            const val META_PREFIX = "announcify"
            const val META_API_KEY_KEY = "${META_PREFIX}_api_key"
            const val META_PROJECT_ID_KEY = "${META_PREFIX}_project_id"
        }

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
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val metaData = applicationInfo.metaData

            val host = host ?: DEFAULT_HOST
            val apiKey = when {
                    apiKey != null -> apiKey!!
                    metaData.getString(META_API_KEY_KEY) != null -> metaData.getString(
                        META_API_KEY_KEY)
                    else ->  throw IllegalArgumentException("Field `apiKey` is required and must be set!")
                }!!
            val projectId = when {
                    projectId != null -> projectId
                    metaData.getInt(META_PROJECT_ID_KEY, -1) != -1 -> metaData.getInt(
                        META_PROJECT_ID_KEY)
                    else ->  throw IllegalArgumentException("Field `projectId` is required and must be set!")
                }!!
            val locale = locale ?: Locale.getDefault()
            val listener = listener ?: throw IllegalArgumentException("Field `resultListener` is required and must be set!")

            return AnnouncifyClient(host, apiKey, projectId, locale, listener)
        }
    }

    interface ResultListener {
        fun onMessage(announcement: Announcement)
        fun onNoMessage()
        fun onFail(e: Exception)
    }
}