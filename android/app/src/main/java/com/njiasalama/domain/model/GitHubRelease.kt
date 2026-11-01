package com.njiasalama.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Model representing an asset attached to a GitHub release (e.g. an APK file).
 */
data class GitHubReleaseAsset(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long,
    @SerializedName("browser_download_url") val downloadUrl: String
)

/**
 * Model representing a published release returned from GitHub REST API.
 */
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubReleaseAsset> = emptyList()
)

/**
 * Represents the parsed update availability result.
 */
sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseNotes: String,
        val apkDownloadUrl: String?,
        val apkSizeMb: Double,
        val releasePageUrl: String
    ) : UpdateCheckResult

    object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

