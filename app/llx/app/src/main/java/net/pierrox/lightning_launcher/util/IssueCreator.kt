package net.pierrox.lightning_launcher.util

import android.content.Intent
import android.net.Uri

object IssueCreator {
    const val ISSUE_TITLE = "github_issue_title"
    const val ISSUE_BODY = "github_issue_body"

    private const val ISSUE_URL = "https://github.com/TrianguloY/LightningLauncher/issues/new"

    fun genIntent(title: String, body: String): Intent {
        val uri = Uri.parse(ISSUE_URL)
            .buildUpon()
            .appendQueryParameter("title", Uri.encode(title))
            .appendQueryParameter("body", Uri.encode(body))
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri)

        return Intent.createChooser(intent, null)
    }

}