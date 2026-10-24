package com.jongh0.bassshot.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.jongh0.bassshot.BuildConfig
import com.jongh0.bassshot.data.remote.CreateIssueRequest
import com.jongh0.bassshot.data.remote.CreateIssueResponse
import com.jongh0.bassshot.data.remote.GitHubIssueApiService
import com.jongh0.bassshot.data.remote.UploadContentRequest
import com.jongh0.bassshot.domain.model.IssueReportInput
import com.jongh0.bassshot.domain.model.IssueReportType
import com.jongh0.bassshot.domain.util.AppLogger
import com.jongh0.bassshot.domain.util.IssueReportFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class IssueReportRepository(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val apiService: GitHubIssueApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubIssueApiService::class.java)
    }

    /**
     * GitHub PAT 토큰 획득
     */
    fun getGitHubToken(): String {
        return BuildConfig.GH_ISSUE_TOKEN.trim()
    }

    fun hasValidToken(): Boolean {
        return getGitHubToken().isNotBlank()
    }

    /**
     * 원본/계측 이미지 업로드 후 GitHub 이슈 등록
     * @param onProgress 진행 상황 메시지 콜백
     */
    suspend fun submitReport(
        input: IssueReportInput,
        onProgress: (String) -> Unit = {}
    ): Result<CreateIssueResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val token = getGitHubToken()
            if (token.isBlank()) {
                throw IllegalStateException("GitHub 토큰(GH_ISSUE_TOKEN)이 설정되어 있지 않습니다.")
            }
            val authHeader = "Bearer $token"
            val owner = BuildConfig.GITHUB_REPO_OWNER
            val repo = BuildConfig.GITHUB_REPO_NAME
            val timePrefix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

            AppLogger.i("IssueReport", "Starting issue report submission (type: ${input.reportType})")

            // 1. 원본 이미지 업로드 (있는 경우)
            var originalImageUrl: String? = null
            if (input.originalBitmap != null) {
                onProgress("원본 이미지 업로드 중...")
                AppLogger.d("IssueReport", "Compressing & uploading original bitmap...")
                val origBytes = compressBitmap(input.originalBitmap, maxDimension = 1600, quality = 85)
                val path = ".github/issue-attachments/${timePrefix}_orig.jpg"
                val uploaded = uploadImageToGitHub(owner, repo, path, origBytes, "Upload original image for issue report", authHeader)
                originalImageUrl = uploaded
                AppLogger.i("IssueReport", "Original image uploaded: $originalImageUrl")
            }

            // 2. 계측 합성본 이미지 업로드 (있는 경우)
            var compositeImageUrl: String? = null
            if (input.compositeBitmap != null) {
                onProgress("계측 이미지 업로드 중...")
                AppLogger.d("IssueReport", "Compressing & uploading composite bitmap...")
                val compBytes = compressBitmap(input.compositeBitmap, maxDimension = 1600, quality = 85)
                val path = ".github/issue-attachments/${timePrefix}_composite.jpg"
                val uploaded = uploadImageToGitHub(owner, repo, path, compBytes, "Upload composite image for issue report", authHeader)
                compositeImageUrl = uploaded
                AppLogger.i("IssueReport", "Composite image uploaded: $compositeImageUrl")
            }

            // 3. 이슈 본문 및 제목 조합
            onProgress("이슈 내용 작성 및 등록 중...")
            val issueTitle = IssueReportFormatter.buildIssueTitle(input)
            val issueBody = IssueReportFormatter.buildIssueBody(context, input, originalImageUrl, compositeImageUrl)

            val labels = when (input.reportType) {
                IssueReportType.MEASUREMENT_INACCURATE -> listOf("bug", "ai-measurement")
                IssueReportType.MEASUREMENT_FAILED -> listOf("bug", "ai-vision-failure")
                IssueReportType.SAVED_PHOTO -> listOf("bug", "photo-viewer")
                IssueReportType.GENERAL -> listOf("bug", "user-feedback")
            }

            // 4. GitHub Issue 생성
            AppLogger.i("IssueReport", "Sending createIssue request to GitHub API...")
            val response = apiService.createIssue(
                owner = owner,
                repo = repo,
                authHeader = authHeader,
                request = CreateIssueRequest(
                    title = issueTitle,
                    body = issueBody,
                    labels = labels
                )
            )

            AppLogger.i("IssueReport", "GitHub issue created successfully: #${response.number} (${response.htmlUrl})")
            response
        }.onFailure { e ->
            AppLogger.e("IssueReport", "Failed to submit issue report: ${e.message}", e)
        }
    }

    private suspend fun uploadImageToGitHub(
        owner: String,
        repo: String,
        path: String,
        imageBytes: ByteArray,
        commitMessage: String,
        authHeader: String
    ): String {
        val base64Content = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val uploadReq = UploadContentRequest(
            message = commitMessage,
            content = base64Content
        )

        val res = apiService.uploadFile(
            owner = owner,
            repo = repo,
            path = path,
            authHeader = authHeader,
            request = uploadReq
        )

        return res.content?.downloadUrl 
            ?: "https://raw.githubusercontent.com/$owner/$repo/main/$path"
    }

    private fun compressBitmap(bitmap: Bitmap, maxDimension: Int, quality: Int): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val scale = if (width > maxDimension || height > maxDimension) {
            val maxSide = maxOf(width, height)
            maxDimension.toFloat() / maxSide.toFloat()
        } else {
            1.0f
        }

        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)

        val scaled = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()

        if (scaled != bitmap && !scaled.isRecycled) {
            scaled.recycle()
        }

        return bytes
    }
}
