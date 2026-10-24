package com.jongh0.bassshot.domain.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.jongh0.bassshot.BuildConfig
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 기기 내 로컬 텍스트 로그 파일(.log)을 기록하고 관리하는 싱글톤 로거.
 * 
 * - 날짜별 분리 저장 (예: bassshot_2026-10-24.log)
 * - 비동기 순차 I/O로 메인 UI 스레드 영향 없음
 * - 미처리 예외(크래시) 자동 감지 및 스택 트레이스 즉시 기록
 * - 보관 기간(7일) 초과 로그 자동 정리
 * - 향후 이슈 리포트 기능에서 로그 파일 추출/공유/조회 API 제공
 */
object AppLogger {

    private const val LOG_DIR_NAME = "logs"
    private const val LOG_FILE_PREFIX = "bassshot_"
    private const val LOG_FILE_EXT = ".log"
    private const val MAX_LOG_RETENTION_DAYS = 7

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val logExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AppLogger-Worker").apply { isDaemon = true }
    }

    @Volatile
    private var logDir: File? = null

    @Volatile
    private var isInitialized = false

    /**
     * Application 시작 시 1회 호출하여 로거를 초기화합니다.
     */
    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext

        val dir = File(appContext.filesDir, LOG_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
        logDir = dir
        isInitialized = true

        // 1. 오래된 로그 파일 자동 청소
        cleanOldLogs()

        // 2. 미처리 예외(Crash) 크래시 핸들러 등록
        setupCrashHandler()

        // 3. 앱 세션 시작 로그 기록
        i("AppLogger", "==================================================")
        i("AppLogger", "BassShot Session Started")
        i("AppLogger", getDeviceInfo(appContext))
        i("AppLogger", "==================================================")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLog("DEBUG", tag, message, null)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog("INFO", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        writeLog("WARN", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeLog("ERROR", tag, message, throwable)
    }

    fun fatal(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, "FATAL: $message", throwable)
        writeLog("FATAL", tag, message, throwable)
    }

    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable?) {
        val now = Date()
        val timestamp = synchronized(timeFormat) { timeFormat.format(now) }
        val dateStr = synchronized(fileDateFormat) { fileDateFormat.format(now) }

        val logLine = buildString {
            append("$timestamp [$level] [$tag] $message\n")
            if (throwable != null) {
                append(getStackTraceString(throwable))
                append("\n")
            }
        }

        val dir = logDir ?: return
        logExecutor.execute {
            runCatching {
                val logFile = File(dir, "$LOG_FILE_PREFIX$dateStr$LOG_FILE_EXT")
                FileWriter(logFile, true).use { writer ->
                    writer.write(logLine)
                }
            }
        }
    }

    /**
     * 처리되지 않은 예외(크래시) 발생 시 로그 파일에 즉시 동기 기록 후 기본 핸들러로 위임
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                fatal(
                    "CrashHandler",
                    "FATAL CRASH in thread '${thread.name}': ${throwable.message ?: throwable.javaClass.simpleName}",
                    throwable
                )
                // 크래시 직전 큐에 남은 로그 동기 flush
                flushSync()
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * 현재 큐에 등록된 로그 I/O 작업을 동기식으로 대기
     */
    fun flushSync(timeoutMs: Long = 1500L) {
        val future = logExecutor.submit { /* 큐의 작업 모두 완료 대기 */ }
        runCatching { future.get(timeoutMs, TimeUnit.MILLISECONDS) }
    }

    /**
     * 보관 기간(7일)이 초과된 오래된 로그 파일 정리
     */
    private fun cleanOldLogs() {
        logExecutor.execute {
            runCatching {
                val dir = logDir ?: return@execute
                val cutoffTime = System.currentTimeMillis() - (MAX_LOG_RETENTION_DAYS * 24L * 60L * 60L * 1000L)
                dir.listFiles { file ->
                    file.isFile && file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(LOG_FILE_EXT)
                }?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * 저장된 로그 파일 목록 반환 (최신 수정일자순 정렬)
     */
    fun getLogFiles(): List<File> {
        val dir = logDir ?: return emptyList()
        return dir.listFiles { file ->
            file.isFile && file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(LOG_FILE_EXT)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 가장 최근 로그 파일 반환
     */
    fun getLatestLogFile(): File? = getLogFiles().firstOrNull()

    /**
     * 최근 N개 라인의 로그 텍스트를 추출 (이슈 리포트 텍스트 첨부용)
     */
    fun getRecentLogsText(maxLines: Int = 300): String {
        flushSync(500L)
        val files = getLogFiles()
        if (files.isEmpty()) return "기록된 로그가 없습니다."

        val lines = mutableListOf<String>()
        for (file in files) {
            runCatching {
                val fileLines = file.readLines()
                lines.addAll(0, fileLines) // 시간순으로 누적
            }
            if (lines.size >= maxLines) break
        }

        val resultLines = if (lines.size > maxLines) {
            lines.takeLast(maxLines)
        } else {
            lines
        }

        return resultLines.joinToString("\n")
    }

    /**
     * 로그 파일을 FileProvider를 통해 공유할 수 있는 content:// Uri 반환
     */
    fun getLogFileUri(context: Context, file: File): Uri? {
        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrNull()
    }

    /**
     * 저장된 모든 로그 파일 삭제
     */
    fun clearAllLogs() {
        logExecutor.execute {
            runCatching {
                val dir = logDir ?: return@execute
                dir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    /**
     * 이슈 리포트에 유용한 디바이스 및 앱 환경 정보 문자열 생성
     */
    fun getDeviceInfo(context: Context): String {
        return buildString {
            append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})\n")
            append("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("Build Fingerprint: ${Build.FINGERPRINT}")
        }
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString().trimEnd()
    }
}
