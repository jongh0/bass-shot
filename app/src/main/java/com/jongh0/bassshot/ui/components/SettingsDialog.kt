package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jongh0.bassshot.ui.theme.AnodizedCharcoal
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.ObsidianBlack
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.SafetyOrange
import com.jongh0.bassshot.ui.theme.SubtleTitanium
import com.jongh0.bassshot.ui.theme.TitaniumSilver
import com.jongh0.bassshot.ui.theme.WarmGraphite

@Composable
fun SettingsDialog(
    currentGloveWidthCm: Float,
    currentSaveOriginal: Boolean,
    onDismissRequest: () -> Unit,
    onSaveSettings: (gloveWidthCm: Float, saveOriginal: Boolean) -> Unit,
    onReportGeneralIssue: (() -> Unit)? = null
) {
    var gloveWidthText by remember { mutableStateOf(currentGloveWidthCm.toString()) }
    var saveOriginalState by remember { mutableStateOf(currentSaveOriginal) }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AnodizedCharcoal)
                .border(1.dp, SubtleTitanium, RoundedCornerShape(16.dp))
                .padding(22.dp)
        ) {
            // 헤더
            Text(
                text = "손 크기 및 앱 설정",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color = OpticalWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 손 기준 폭 설정
            Text(
                text = "주먹 쥔 손 기준 폭 (cm)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MutedSilver
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "배스 입을 잡았을 때 손가락 너클 전체의 실제 상하 폭을 입력하세요. (기본값: 9.5cm)",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = WarmGraphite
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = gloveWidthText,
                    onValueChange = { input ->
                        gloveWidthText = input
                        val parsed = input.toFloatOrNull()
                        hasError = parsed == null || parsed !in 5.0f..20.0f
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = hasError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MutedSilver,
                        unfocusedTextColor = MutedSilver,
                        focusedBorderColor = TitaniumSilver,
                        unfocusedBorderColor = SubtleTitanium,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        gloveWidthText = "9.5"
                        hasError = false
                    }
                ) {
                    Text("초기화", color = TitaniumSilver, fontSize = 12.sp)
                }
            }

            if (hasError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "5.0 ~ 20.0 cm 사이의 올바른 숫자를 입력하세요.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 원본 사진 함께 저장 옵션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "원본 사진 저장",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MutedSilver
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "원본 사진을 함께 저장합니다.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = WarmGraphite
                    )
                }

                Switch(
                    checked = saveOriginalState,
                    onCheckedChange = { saveOriginalState = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TitaniumSilver,
                        checkedTrackColor = ObsidianBlack,
                        uncheckedThumbColor = WarmGraphite,
                        uncheckedTrackColor = AnodizedCharcoal
                    )
                )
            }

            if (onReportGeneralIssue != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AnodizedCharcoal)
                        .border(1.dp, SubtleTitanium, RoundedCornerShape(8.dp))
                        .clickable {
                            onDismissRequest()
                            onReportGeneralIssue()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = SafetyOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "문제 제보 및 피드백",
                            color = MutedSilver,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "GitHub 이슈 등록",
                        color = WarmGraphite,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 버튼 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("취소", color = WarmGraphite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val parsed = gloveWidthText.toFloatOrNull()?.coerceIn(5.0f, 20.0f) ?: 9.5f
                        onSaveSettings(parsed, saveOriginalState)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TitaniumSilver,
                        contentColor = ObsidianBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("설정 저장", fontWeight = FontWeight.Bold, color = ObsidianBlack)
                }
            }
        }
    }
}
