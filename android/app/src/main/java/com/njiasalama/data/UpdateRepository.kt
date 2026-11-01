package com.njiasalama.data

import com.njiasalama.domain.model.GitHubRelease
import com.njiasalama.domain.model.UpdateCheckResult
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Handles checking for newer application releases from GitHub with built-in rate-limiting and caching.
 */
class UpdateRepository(
    private val apiService: GitHubApiService,
    private val cacheDir: File,
    private val currentVersionName: String
) {

    private val cacheTimestampFile = File(cacheDir, "last_update_check.timestamp")
    private val cacheIntervalMillis = TimeUnit.HOURS.toMillis(4) // Check at most once every 4 hours

    /**
     * Checks if a new update is available on GitHub.
     * @param forceCheck If true, bypasses the local rate-limiting cache.
     */
    suspend fun checkForUpdates(forceCheck: Boolean = false): UpdateCheckResult {
        val now = System.currentTimeMillis()
        if (!forceCheck && isRateLimited(now)) {
            return UpdateCheckResult.UpToDate
        }

        return try {
            val release = apiService.getLatestRelease()
            recordCheckTimestamp(now)

            if (isNewerVersion(remoteTag = release.tagName, currentVersion = currentVersionName)) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                val sizeMb = if (apkAsset != null) (apkAsset.size.toDouble() / (1024 * 1024)) else 0.0

                UpdateCheckResult.UpdateAvailable(
                    latestVersion = release.tagName,
                    releaseNotes = release.body ?: "New performance improvements and bug fixes.",
                    apkDownloadUrl = apkAsset?.downloadUrl,
                    apkSizeMb = sizeMb,
                    releasePageUrl = release.htmlUrl
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Failed to check for updates")
        }
    }

    private fun isRateLimited(now: Long): Boolean {
        return try {
            if (!cacheTimestampFile.exists()) return false
            val lastCheck = cacheTimestampFile.readText().toLongOrNull() ?: 0L
            (now - lastCheck) < cacheIntervalMillis
        } catch (e: Exception) {
            false
        }
    }

    private fun recordCheckTimestamp(now: Long) {
        try {
            cacheTimestampFile.writeText(now.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * Compares semantic versions (e.g., "v1.0.4" vs "v1.0.3-alpha.1").
         * Returns true if remoteTag represents a newer version than currentVersion.
         */
        fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
            val remoteParts = extractVersionNumbers(remoteTag)
            val currentParts = extractVersionNumbers(currentVersion)

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }

            // If main semantic digits are identical, check pre-release suffix precedence
            // e.g. "1.0.3" is newer than "1.0.3-alpha.1"
            val remoteIsPre = remoteTag.contains("alpha", ignoreCase = true) || remoteTag.contains("beta", ignoreCase = true)
            val currentIsPre = currentVersion.contains("alpha", ignoreCase = true) || currentVersion.contains("beta", ignoreCase = true)

            return (!remoteIsPre && currentIsPre)
        }

        private fun extractVersionNumbers(versionStr: String): List<Int> {
            val cleaned = versionStr.trimStart('v', 'V').split("-")[0]
            return cleaned.split(".")
                .mapNotNull { it.toIntOrNull() }
        }
    }
}

