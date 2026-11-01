package com.njiasalama.data

import com.njiasalama.domain.model.GitHubRelease
import com.njiasalama.domain.model.GitHubReleaseAsset
import com.njiasalama.domain.model.UpdateCheckResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockApi: FakeGitHubApiService

    class FakeGitHubApiService : GitHubApiService {
        var releaseToReturn: GitHubRelease? = null
        var shouldThrowError = false
        var callCount = 0

        override suspend fun getLatestRelease(owner: String, repo: String): GitHubRelease {
            callCount++
            if (shouldThrowError) throw RuntimeException("GitHub 404 or network error")
            return releaseToReturn ?: throw IllegalStateException("Release not mocked")
        }
    }

    @Before
    fun setUp() {
        mockApi = FakeGitHubApiService()
    }

    @Test
    fun test_newer_version_triggers_update_available() = runTest {
        mockApi.releaseToReturn = GitHubRelease(
            tagName = "v1.0.4",
            name = "NjiaSalama v1.0.4",
            body = "Added post-ride hazard reporting and cyclist proximity alerts.",
            publishedAt = "2026-11-01T12:00:00Z",
            htmlUrl = "https://github.com/wilfredkimura/njiasalama2.0/releases/tag/v1.0.4",
            assets = listOf(
                GitHubReleaseAsset(
                    name = "NjiaSalama-v1.0.4.apk",
                    size = 15728640L, // ~15 MB
                    downloadUrl = "https://github.com/wilfredkimura/njiasalama2.0/releases/download/v1.0.4/NjiaSalama-v1.0.4.apk"
                )
            )
        )

        val repo = UpdateRepository(
            apiService = mockApi,
            cacheDir = tempFolder.root,
            currentVersionName = "1.0.3-alpha.2"
        )

        val result = repo.checkForUpdates(forceCheck = true)

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val update = result as UpdateCheckResult.UpdateAvailable
        assertEquals("v1.0.4", update.latestVersion)
        assertEquals(15.0, update.apkSizeMb, 0.1)
        assertNotNull(update.apkDownloadUrl)
    }

    @Test
    fun test_older_or_equal_version_returns_no_update() = runTest {
        mockApi.releaseToReturn = GitHubRelease(
            tagName = "v1.0.2",
            name = "Old Release",
            body = null,
            publishedAt = null,
            htmlUrl = "https://github.com",
            assets = emptyList()
        )

        val repo = UpdateRepository(
            apiService = mockApi,
            cacheDir = tempFolder.root,
            currentVersionName = "1.0.3-alpha.2"
        )

        val result = repo.checkForUpdates(forceCheck = true)
        assertTrue(result is UpdateCheckResult.UpToDate)
    }

    @Test
    fun test_rate_limiting_cache_prevents_unnecessary_calls() = runTest {
        mockApi.releaseToReturn = GitHubRelease(
            tagName = "v1.0.3",
            name = "Current Release",
            body = null,
            publishedAt = null,
            htmlUrl = "https://github.com",
            assets = emptyList()
        )

        val repo = UpdateRepository(
            apiService = mockApi,
            cacheDir = tempFolder.root,
            currentVersionName = "1.0.3"
        )

        // First check hits the API
        repo.checkForUpdates(forceCheck = false)
        assertEquals(1, mockApi.callCount)

        // Second check within interval returns UpToDate without calling API again
        val secondResult = repo.checkForUpdates(forceCheck = false)
        assertTrue(secondResult is UpdateCheckResult.UpToDate)
        assertEquals(1, mockApi.callCount) // No second API call!
    }

    @Test
    fun test_semantic_version_comparison_logic() {
        assertTrue(UpdateRepository.isNewerVersion("v1.0.4", "1.0.3"))
        assertTrue(UpdateRepository.isNewerVersion("v1.1.0", "1.0.9"))
        assertTrue(UpdateRepository.isNewerVersion("v2.0.0", "1.9.9"))
        assertTrue(UpdateRepository.isNewerVersion("v1.0.3", "1.0.3-alpha.1")) // Stable is newer than alpha

        assertFalse(UpdateRepository.isNewerVersion("v1.0.3", "1.0.3"))
        assertFalse(UpdateRepository.isNewerVersion("v1.0.2", "1.0.3"))
        assertFalse(UpdateRepository.isNewerVersion("v1.0.0", "1.0.1"))
    }
}

