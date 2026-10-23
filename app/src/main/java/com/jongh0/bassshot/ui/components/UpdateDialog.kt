package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jongh0.bassshot.ui.theme.AnodizedCharcoal
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.ObsidianBlack
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.SubtleTitanium
import com.jongh0.bassshot.ui.theme.TitaniumSilver
import com.jongh0.bassshot.ui.theme.WarmGraphite

@Composable
fun UpdateDialog(
    versionName: String,
    onDismissRequest: () -> Unit,
    onConfirmUpdate: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AnodizedCharcoal)
                .border(1.dp, SubtleTitanium, RoundedCornerShape(16.dp))
                .padding(22.dp)
        ) {
            Text(
                text = "새 버전 업데이트 안내",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color = OpticalWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "배스샷 최신 버전($versionName)이 출시되었습니다.\n지금 업데이트하여 최신 AI 모델과 개선 사항을 적용하시겠습니까?",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSilver,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("나중에", color = WarmGraphite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TitaniumSilver,
                        contentColor = ObsidianBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("업데이트", fontWeight = FontWeight.Bold, color = ObsidianBlack)
                }
            }
        }
    }
}
