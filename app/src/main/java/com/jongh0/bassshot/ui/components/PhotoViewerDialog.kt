package com.jongh0.bassshot.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jongh0.bassshot.R
import com.jongh0.bassshot.domain.model.SavedPhotoItem
import com.jongh0.bassshot.ui.theme.AnodizedCharcoal
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.SubtleTitanium
import com.jongh0.bassshot.ui.theme.TitaniumSilver
import com.jongh0.bassshot.ui.theme.WarmGraphite

@Composable
fun PhotoViewerDialog(
    photos: List<SavedPhotoItem>,
    initialIndex: Int = 0,
    onDismissRequest: () -> Unit,
    onDeletePhoto: (SavedPhotoItem) -> Unit,
    onMeasurePhoto: ((SavedPhotoItem, Int) -> Unit)? = null
) {
    if (photos.isEmpty()) {
        onDismissRequest()
        return
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.size - 1),
        pageCount = { photos.size }
    )

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 안드로이드 시스템 뒤로가기 제스처 및 하드웨어 뒤로가기 버튼 처리
    BackHandler(onBack = onDismissRequest)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 상단 탑바 (닫기 버튼 및 페이지 인디케이터)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = OpticalWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        color = OpticalWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // 우측 균형용 투명 스페이서
                    Spacer(modifier = Modifier.size(40.dp))
                }

                // 2. 중앙 이미지 스와이프 페이저
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val photo = photos.getOrNull(page)
                    if (photo != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photo.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = photo.displayName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 3. 하단 액션 바 (카카오톡 전송, 공유, AI 계측, 삭제 - 내비게이션 바 위 96dp 수직 중앙 정렬)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentPhoto = photos.getOrNull(pagerState.currentPage)

                        // [액션 1] 카카오톡 전송 버튼 (실제 카카오톡 공식 브랜드 아이콘)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (currentPhoto != null) {
                                        val kakaoIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/jpeg"
                                            putExtra(Intent.EXTRA_STREAM, currentPhoto.uri)
                                            setPackage("com.kakao.talk")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(kakaoIntent)
                                        } catch (e: ActivityNotFoundException) {
                                            val chooser = Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/jpeg"
                                                    putExtra(Intent.EXTRA_STREAM, currentPhoto.uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                },
                                                "카카오톡 공유"
                                            )
                                            context.startActivity(chooser)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_kakaotalk),
                                contentDescription = "카카오톡 공유",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // [액션 2] 더 보기 (시스템 공유 시트) 버튼
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AnodizedCharcoal)
                                .border(1.dp, SubtleTitanium, CircleShape)
                                .clickable {
                                    if (currentPhoto != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/jpeg"
                                            putExtra(Intent.EXTRA_STREAM, currentPhoto.uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val chooser = Intent.createChooser(shareIntent, "사진 공유")
                                        context.startActivity(chooser)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "더 보기",
                                tint = TitaniumSilver,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // [액션 3] AI 계측 버튼 (삭제 버튼 좌측 배치, 일반 모드로 찍은 사진을 뷰어에서 바로 계측으로 전환)
                        // 이미 계측 정보가 포함된 사진은 계측 버튼 숨김
                        if (onMeasurePhoto != null && currentPhoto != null && !currentPhoto.isAlreadyMeasured) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AnodizedCharcoal)
                                    .border(1.dp, SubtleTitanium, CircleShape)
                                    .clickable {
                                        onMeasurePhoto(currentPhoto, pagerState.currentPage)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = "AI 계측하기",
                                    tint = OpticalWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // [액션 4] 삭제 버튼
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AnodizedCharcoal)
                                .border(1.dp, SubtleTitanium, CircleShape)
                                .clickable {
                                    showDeleteConfirmDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "삭제",
                                tint = Color(0xFFFF5C5C),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 삭제 확인 대화상자
            if (showDeleteConfirmDialog) {
                val currentPhoto = photos.getOrNull(pagerState.currentPage)
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text(
                            text = "사진 삭제",
                            color = OpticalWhite,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "이 사진을 갤러리에서 영구히 삭제하시겠습니까?",
                            color = MutedSilver,
                            fontSize = 13.5.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                if (currentPhoto != null) {
                                    onDeletePhoto(currentPhoto)
                                }
                            }
                        ) {
                            Text("삭제", color = Color(0xFFFF5C5C), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("취소", color = WarmGraphite)
                        }
                    },
                    containerColor = AnodizedCharcoal,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
