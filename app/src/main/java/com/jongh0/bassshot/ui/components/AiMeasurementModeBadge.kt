package com.jongh0.bassshot.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jongh0.bassshot.R
import com.jongh0.bassshot.ui.theme.FrostedGlassBg
import com.jongh0.bassshot.ui.theme.FrostedGlassBorder
import com.jongh0.bassshot.ui.theme.OpticalWhite

/**
 * 실시간 카메라 프리뷰 우상단에 표시되는 AI 계측 모드 인디케이터 배지.
 * [세로 배스 + 눈금자] 형태의 라운드 스퀘어 카드 디자인.
 */
@Composable
fun AiMeasurementModeBadge(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FrostedGlassBg)
                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_fish_ruler_measure),
                contentDescription = "AI 계측 모드 활성화",
                tint = OpticalWhite,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
