package com.jongh0.bassshot.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = emptyList()
)

@Serializable
data class CreateIssueResponse(
    val id: Long,
    val number: Int,
    @SerialName("html_url") val htmlUrl: String,
    val title: String,
    val state: String = "open"
)

@Serializable
data class UploadContentRequest(
    val message: String,
    val content: String, // Base64 encoded file content
    val branch: String? = null
)

@Serializable
data class UploadContentResponse(
    val content: ContentFileInfo? = null
)

@Serializable
data class ContentFileInfo(
    val name: String,
    val path: String,
    val sha: String,
    @SerialName("download_url") val downloadUrl: String? = null
)

interface GitHubIssueApiService {

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authHeader: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("X-GitHub-Api-Version") apiVersion: String = "2022-11-28",
        @Body request: CreateIssueRequest
    ): CreateIssueResponse

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun uploadFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Header("Authorization") authHeader: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("X-GitHub-Api-Version") apiVersion: String = "2022-11-28",
        @Body request: UploadContentRequest
    ): UploadContentResponse
}
