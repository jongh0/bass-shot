package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.R
import com.jongh0.bassshot.domain.model.EnvironmentData
import com.jongh0.bassshot.ui.theme.FrostedGlassBg
import com.jongh0.bassshot.ui.theme.OpticalWhite

@Composable
fun TimemarkOverlay(
    environmentData: EnvironmentData,
    modifier: Modifier = Modifier
) {
    val currentDensity = LocalDensity.current

    // 카메라 HUD 오버레이는 시스템 글꼴 배율(fontScale) 설정과 무관하게 1.0f로 고정하여 레이아웃 깨짐 원천 방지
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = 1.0f
        )
    ) {
        // 텍스트 드롭 섀도우 (밝은 수면/야외에서도 시인성 보장)
        val textShadow = Shadow(
            color = Color(0xD9000000),
            offset = Offset(0f, 2f),
            blurRadius = 6f
        )

        Box(modifier = modifier.fillMaxSize()) {
        // 1. 좌상단 날씨/바람/기압 카드 (직사각형 모서리 + 좌상단 끝 밀착)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(140.dp)
                .background(FrostedGlassBg)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1줄: 날씨 및 기온
                Row(
                    modifier = Modifier.height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_weather_cloud),
                        contentDescription = null,
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = environmentData.formattedWeatherLine1(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = OpticalWhite
                    )
                }

                // 2줄: 바람
                Row(
                    modifier = Modifier.height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wind),
                        contentDescription = null,
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = environmentData.formattedWeatherLine2(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = OpticalWhite
                    )
                }

                // 3줄: 기압
                Row(
                    modifier = Modifier.height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_barometer),
                        contentDescription = null,
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = environmentData.formattedWeatherLine3(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = OpticalWhite
                    )
                }
            }
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
                // 1. 시간 (42sp Bold)
                Text(
                    text = environmentData.timeFormatted,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        lineHeight = 42.sp,
                        letterSpacing = (-0.03).sp,
                        fontFeatureSettings = "tnum",
                        shadow = textShadow
                    ),
                    color = OpticalWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. 날짜 및 요일: "2026. 10. 24 (토)"
                val dateAndDay = environmentData.formattedDateAndDayClean()
                if (dateAndDay.isNotEmpty()) {
                    Text(
                        text = dateAndDay,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = (-0.01).sp,
                            shadow = textShadow
                        ),
                        color = OpticalWhite
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }

                // 3. 주소: 앞에 핀 아이콘 ic_location_pin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_pin),
                        contentDescription = null,
                        tint = OpticalWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = environmentData.formattedAddressOnly(),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            shadow = textShadow
                        ),
                        color = OpticalWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
}
