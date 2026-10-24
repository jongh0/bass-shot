package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jongh0.bassshot.domain.model.IssueReportType
import com.jongh0.bassshot.ui.theme.AnodizedCharcoal
import com.jongh0.bassshot.ui.theme.GunmetalGrey
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.ObsidianBlack
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.SafetyOrange
import com.jongh0.bassshot.ui.theme.SmokeGlass
import com.jongh0.bassshot.ui.theme.SubtleTitanium
import com.jongh0.bassshot.ui.theme.TitaniumSilver
import com.jongh0.bassshot.ui.theme.WarmGraphite

@Composable
fun IssueReportDialog(
    reportType: IssueReportType,
    hasValidToken: Boolean,
    isSubmitting: Boolean,
    submitProgress: String?,
    createdIssueUrl: String?,
    errorMessage: String?,
    hasMeasurement: Boolean,
    onDismissRequest: () -> Unit,
    onSubmit: (comment: String, actualLength: Float?, actualWeight: Int?, includeLogs: Boolean) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var actualLengthStr by remember { mutableStateOf("") }
    var actualWeightStr by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBlack)
                .border(1.dp, SubtleTitanium, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 헤더: 아이콘 + 제목 + 닫기 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafetyOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = SafetyOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = reportType.label,
                            color = OpticalWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isSubmitting) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = WarmGraphite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 성공 상태 뷰
                if (createdIssueUrl != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GitHub 이슈 등록 완료!",
                            color = OpticalWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "제보해주신 데이터는 AI 계측 모델 성능 개선에 소중히 활용됩니다.",
                            color = MutedSilver,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 확인 버튼
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TitaniumSilver)
                                .clickable { onDismissRequest() }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "확인",
                                color = ObsidianBlack,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    return@Column
                }

                // 3. 토큰 미설정 경고 배너
                if (!hasValidToken) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3E2723))
                            .border(1.dp, Color(0xFFD84315), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GitHub 토큰(GH_ISSUE_TOKEN)이 설정되지 않아 이슈를 등록할 수 없습니다.",
                            color = Color(0xFFFFCCBC),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 4. 에러 메시지 표시
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = SafetyOrange,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // 5. 안내 설명
                Text(
                    text = "발생한 문제점을 간단히 적어주시면 원본 사진, 계측 데이터, 시스템 로그와 함께 GitHub 이슈로 전송되어 AI 정확도 개선에 반영됩니다.",
                    color = WarmGraphite,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 6. 문제 설명 입력창 (필수)
                Text(
                    text = "문제 내용 (필수)",
                    color = OpticalWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = {
                        Text(
                            text = when (reportType) {
                                IssueReportType.MEASUREMENT_INACCURATE -> "예) 실제 길이는 약 40cm인데 28cm로 나옵니다 / 꼬리 끝이 아니라 몸통통통 중간을 잡았습니다."
                                IssueReportType.MEASUREMENT_FAILED -> "예) 배스를 들고 있는데 물고기를 아예 인식하지 못합니다."
                                IssueReportType.SAVED_PHOTO -> "예) 뷰어에서 확인해보니 계측선 위치가 이상합니다."
                                IssueReportType.GENERAL -> "예) 날씨 및 기압 정보가 실제와 다릅니다."
                            },
                            color = WarmGraphite,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OpticalWhite,
                        unfocusedTextColor = OpticalWhite,
                        focusedBorderColor = TitaniumSilver,
                        unfocusedBorderColor = SubtleTitanium,
                        focusedContainerColor = AnodizedCharcoal,
                        unfocusedContainerColor = AnodizedCharcoal
                    ),
                    enabled = !isSubmitting
                )

                // 7. 실측값 입력창 (계측 관련 제보 시)
                if (reportType != IssueReportType.GENERAL) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "실제 측정한 값 (선택 사항)",
                        color = OpticalWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = actualLengthStr,
                            onValueChange = { actualLengthStr = it },
                            label = { Text("실측 전장 (cm)", fontSize = 11.sp) },
                            placeholder = { Text("예: 38.5", color = WarmGraphite, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpticalWhite,
                                unfocusedTextColor = OpticalWhite,
                                focusedBorderColor = TitaniumSilver,
                                unfocusedBorderColor = SubtleTitanium,
                                focusedContainerColor = AnodizedCharcoal,
                                unfocusedContainerColor = AnodizedCharcoal
                            ),
                            enabled = !isSubmitting,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = actualWeightStr,
                            onValueChange = { actualWeightStr = it },
                            label = { Text("실측 무게 (g)", fontSize = 11.sp) },
                            placeholder = { Text("예: 850", color = WarmGraphite, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpticalWhite,
                                unfocusedTextColor = OpticalWhite,
                                focusedBorderColor = TitaniumSilver,
                                unfocusedBorderColor = SubtleTitanium,
                                focusedContainerColor = AnodizedCharcoal,
                                unfocusedContainerColor = AnodizedCharcoal
                            ),
                            enabled = !isSubmitting,
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8. 자동 포함 데이터 배너
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SmokeGlass)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "자동 첨부:", color = WarmGraphite, fontSize = 11.sp)
                    Text(text = "📷 원본", color = MutedSilver, fontSize = 11.sp)
                    if (hasMeasurement) Text(text = "📏 계측본", color = MutedSilver, fontSize = 11.sp)
                    Text(text = "📱 기기정보", color = MutedSilver, fontSize = 11.sp)
                    Text(text = "🌦️ 환경스펙", color = MutedSilver, fontSize = 11.sp)
                }

                // 9. 로그 포함 체크박스
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSubmitting) { includeLogs = !includeLogs },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeLogs,
                        onCheckedChange = { includeLogs = it },
                        enabled = !isSubmitting,
                        colors = CheckboxDefaults.colors(
                            checkedColor = TitaniumSilver,
                            checkmarkColor = ObsidianBlack,
                            uncheckedColor = WarmGraphite
                        )
                    )
                    Text(
                        text = "최근 시스템/동작 로그 포함 (권장)",
                        color = MutedSilver,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 10. 하단 버튼 영역
                if (isSubmitting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AnodizedCharcoal)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = SafetyOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = submitProgress ?: "GitHub 이슈 등록 중...",
                            color = OpticalWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 취소 버튼
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AnodizedCharcoal)
                                .border(1.dp, SubtleTitanium, RoundedCornerShape(8.dp))
                                .clickable { onDismissRequest() }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "취소",
                                color = WarmGraphite,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 제보 등록 버튼
                        val isCommentValid = comment.trim().length >= 2
                        val canSubmit = isCommentValid && hasValidToken
                        Row(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canSubmit) TitaniumSilver else GunmetalGrey)
                                .clickable(enabled = canSubmit) {
                                    val length = actualLengthStr.toFloatOrNull()
                                    val weight = actualWeightStr.toIntOrNull()
                                    onSubmit(comment.trim(), length, weight, includeLogs)
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = if (canSubmit) ObsidianBlack else WarmGraphite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "이슈 제보 등록",
                                color = if (canSubmit) ObsidianBlack else WarmGraphite,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
