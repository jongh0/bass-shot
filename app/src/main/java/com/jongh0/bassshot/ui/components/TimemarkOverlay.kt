package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.EnvironmentData
import com.jongh0.bassshot.ui.theme.CoolGrey
import com.jongh0.bassshot.ui.theme.HairlineBorder
import com.jongh0.bassshot.ui.theme.LeicaAmber
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.Typography

@Composable
fun TimemarkOverlay(
    environmentData: EnvironmentData,
    modifier: Modifier = Modifier
) {
    // 하단 텍스트 가독성을 위한 반투명 블랙 그라데이션
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x990A0A0C),
                        Color(0xE60A0A0C)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 좌측 하단: 시간 + 앰버 바 + 환경 정보
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // 시간 (예: 14:18)
                Text(
                    text = environmentData.timeFormatted,
                    style = Typography.displayLarge,
                    color = OpticalWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Leica Amber 수직 분할 바
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(38.dp)
                        .background(LeicaAmber, shape = RoundedCornerShape(1.5.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // 날짜, 기상, 주소 메타데이터
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    if (environmentData.dateFormatted.isNotEmpty()) {
                        Text(
                            text = environmentData.dateFormatted,
                            style = Typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = OpticalWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(
                        text = environmentData.formattedWeatherLine(),
                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                        color = CoolGrey
                    )

                    if (!environmentData.addressKorean.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = environmentData.addressKorean,
                            style = Typography.bodyMedium.copy(fontSize = 11.sp),
                            color = CoolGrey,
                            maxLines = 1
                        )
                    }
                }
            }

            // 우측 하단: 둥근 미니 정적 지도 (다운로드 성공 시 표시)
            environmentData.staticMapBitmap?.let { mapBitmap ->
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = mapBitmap.asImageBitmap(),
                        contentDescription = "Mini Static Map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}
