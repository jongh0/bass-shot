package com.jongh0.bassshot.domain.util

import com.jongh0.bassshot.domain.model.IssueReportInput
import com.jongh0.bassshot.domain.model.IssueReportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueReportFormatterTest {

    @Test
    fun testBuildIssueTitle() {
        val input = IssueReportInput(
            reportType = IssueReportType.SAVED_PHOTO,
            userComment = "31cm 맞을까?\n두번째 줄 내용",
            includeLogs = false
        )
        val title = IssueReportFormatter.buildIssueTitle(input)
        assertEquals("[BUG]: 사진 뷰어 계측 오류 - 31cm 맞을까?", title)
    }

    @Test
    fun testBuildIssueTitleTruncation() {
        val longText = "이것은매우매우매우매우매우매우매우매우매우매우매우매우매우긴설명문입니다아아아아아아아아아아아아"
        val input = IssueReportInput(
            reportType = IssueReportType.GENERAL,
            userComment = longText,
            includeLogs = false
        )
        val title = IssueReportFormatter.buildIssueTitle(input)
        assertTrue(title.endsWith("..."))
        assertTrue(title.startsWith("[BUG]: 일반 오류 및 피드백 - "))
    }
}
