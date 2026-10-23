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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.EnvironmentData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.TitaniumSilver
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.WarmGraphite

@Composable
fun TimemarkOverlay(
    environmentData: EnvironmentData,
    modifier: Modifier = Modifier
) {
    // 텍스트 드롭 섀도우 (밝은 수면/야외에서도 시인성 보장)
    val textShadow = Shadow(
        color = Color(0xD9000000),
        offset = Offset(0f, 2f),
        blurRadius = 6f
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. 좌상단 날씨 3줄 정보 (배경 박스 없이 드롭 섀도우로 최상단 좌측에 밀착 배치)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 10.dp, start = 12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 1줄: 날씨 및 기온 (날짜와 동일한 11.5sp, Normal, OpticalWhite 적용)
            Text(
                text = environmentData.formattedWeatherLine1(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    shadow = textShadow
                ),
                color = OpticalWhite
            )
            // 2줄: 바람 (날짜와 동일한 OpticalWhite 적용)
            Text(
                text = environmentData.formattedWeatherLine2(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    shadow = textShadow
                ),
                color = OpticalWhite
            )
            // 3줄: 기압 (날짜와 동일한 OpticalWhite 적용)
            Text(
                text = environmentData.formattedWeatherLine3(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    shadow = textShadow
                ),
                color = OpticalWhite
            )
        }

        // 2. 하단 타임마크 블록 (좌측 시간/날짜/주소, 우측 미니 지도)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // 좌측 타임마크 정보 블록
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                // 1. 시간 (Tall Condensed 38sp, 날짜와 동일한 OpticalWhite 색상 적용)
                Text(
                    text = environmentData.timeFormatted,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.03).sp,
                        fontFeatureSettings = "tnum",
                        shadow = textShadow
                    ),
                    color = OpticalWhite
                )

                Spacer(modifier = Modifier.height(3.dp))

                // 2. 날짜 및 요일 (예: 2026년 10월 21일 (금요일))
                val dateAndDay = environmentData.formattedDateAndDayLine()
                if (dateAndDay.isNotEmpty()) {
                    Text(
                        text = dateAndDay,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = (-0.01).sp,
                            shadow = textShadow
                        ),
                        color = OpticalWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. 주소 단독 줄 (긴 주소도 잘리지 않도록 단독 배치 및 최대 2줄 래핑)
                Text(
                    text = environmentData.formattedAddressOnly(),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        shadow = textShadow
                    ),
                    color = MutedSilver,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 우측: 정적 지도 미니 맵
            environmentData.staticMapBitmap?.let { mapBitmap ->
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(10.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                ) {
                    Image(
                        bitmap = mapBitmap.asImageBitmap(),
                        contentDescription = "Fishing Spot Map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}
