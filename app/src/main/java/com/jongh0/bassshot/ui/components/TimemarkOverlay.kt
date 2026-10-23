package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.EnvironmentData
import com.jongh0.bassshot.ui.theme.LeicaAmber
import com.jongh0.bassshot.ui.theme.OpticalWhite

@Composable
fun TimemarkOverlay(
    environmentData: EnvironmentData,
    modifier: Modifier = Modifier
) {
    // 글자 자체에 은은한 블랙 드롭 섀도우를 주어 배경 박스 없이도 밝은 사진/물가 위에서 선명하게 판독
    val textShadow = Shadow(
        color = Color(0xD9000000),
        offset = Offset(0f, 2f),
        blurRadius = 6f
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // 좌측: 시간 + 날짜(2층) + 하단 주소/날씨
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.Bottom
        ) {
            // 1. 시간 + 옐로우 바 + 2층 날짜/요일
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tall Condensed 시간 (예: 13:26)
                Text(
                    text = environmentData.timeFormatted,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 58.sp,
                        lineHeight = 58.sp,
                        letterSpacing = (-0.04).sp,
                        fontFeatureSettings = "tnum",
                        shadow = textShadow
                    ),
                    color = OpticalWhite
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 세련된 옐로우 수직 분할 바 (얇고 정확한 높이)
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(44.dp)
                        .shadow(2.dp, RoundedCornerShape(1.dp))
                        .clip(RoundedCornerShape(1.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(1.dp))
                    ) {
                        // 앰버 색상
                        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(LeicaAmber)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2층 날짜/요일
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    if (environmentData.dateFormatted.isNotEmpty()) {
                        Text(
                            text = environmentData.dateFormatted,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                letterSpacing = (-0.01).sp,
                                shadow = textShadow
                            ),
                            color = OpticalWhite,
                            softWrap = false
                        )
                    }
                    if (environmentData.dayOfWeekFormatted.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = environmentData.dayOfWeekFormatted,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                letterSpacing = (-0.01).sp,
                                shadow = textShadow
                            ),
                            color = OpticalWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. 하단 주소 및 기상 정보 (전폭을 자연스럽게 활용하여 표시)
            val infoLine = environmentData.formattedAddressAndWeatherLine()
            Text(
                text = infoLine,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    shadow = textShadow
                ),
                color = OpticalWhite,
                maxLines = 2
            )
        }

        // 우측 하단: 시원하게 확대된 미니 정적 지도 (92dp 정사각형)
        environmentData.staticMapBitmap?.let { mapBitmap ->
            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
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
