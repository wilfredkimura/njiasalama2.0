package com.njiasalama.data

import com.njiasalama.domain.model.GitHubRelease
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for fetching release metadata directly from GitHub.
 */
interface GitHubApiService {

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "wilfredkimura",
        @Path("repo") repo: String = "njiasalama2.0"
    ): GitHubRelease
}

