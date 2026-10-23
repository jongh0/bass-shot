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
import com.jongh0.bassshot.ui.theme.CoolGrey
import com.jongh0.bassshot.ui.theme.DarkSurface
import com.jongh0.bassshot.ui.theme.HairlineBorder
import com.jongh0.bassshot.ui.theme.LeicaAmber
import com.jongh0.bassshot.ui.theme.MatteBlack
import com.jongh0.bassshot.ui.theme.OpticalWhite

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
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "NEW VERSION AVAILABLE",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
                color = LeicaAmber,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "BassShot 최신 버전($versionName)이 출시되었습니다.\n지금 업데이트하여 최신 AI 모델과 개선 사항을 적용하시겠습니까?",
                style = MaterialTheme.typography.bodyMedium,
                color = CoolGrey
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("나중에", color = CoolGrey)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeicaAmber,
                        contentColor = MatteBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("업데이트", fontWeight = FontWeight.Bold, color = MatteBlack)
                }
            }
        }
    }
}
