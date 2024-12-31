package fr.youtubedetector

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class TutoFragment : Fragment() {

    val videoLink = "18tVvE_9TXQ"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tuto, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tutorial
        val tutorialTextView = view.findViewById<ImageView>(R.id.youtubeImage)
        tutorialTextView.setOnClickListener {
            openYoutubeLink(videoLink)
        }
    }

    private fun openYoutubeLink(youtubeID: String) {
        val intentApp = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$youtubeID"))
        val intentBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=$youtubeID"))
        try {
            this.startActivity(intentApp)
        } catch (ex: ActivityNotFoundException) {
            this.startActivity(intentBrowser)
        }

    }
}