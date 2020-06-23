package io.announcify.example

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import io.announcify.AnnouncifyClient
import io.announcify.model.Announcement


class MainActivity : Activity() {

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        AnnouncifyClient.Builder()
            .host("api.announcify.io")
            .apiKey("TP1enlANO31faoqf3ihnDDCPRet6rwxV")
            .projectId(1)
            .resultListener(object: AnnouncifyClient.ResultListener {
                override fun onSuccess(announcement: Announcement) {
                    val bannerView: CardView = findViewById(R.id.banner)
                    bannerView.visibility = View.VISIBLE

                    val titleView: TextView = findViewById(R.id.title)
                    titleView.text = announcement.title

                    val messageView: TextView = findViewById(R.id.message)
                    messageView.text = announcement.message

                    val okButton: Button = findViewById(R.id.ok)
                    okButton.setOnClickListener {
                        bannerView.visibility = View.GONE
                    }
                }

                override fun onFail(e: Exception) {
                }

            })
            .build()
            .request()
    }
}