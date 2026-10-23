package com.jongh0.bassshot.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jongh0.bassshot.ui.theme.AnodizedCharcoal
import com.jongh0.bassshot.ui.theme.ChampagneGold
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.ObsidianBlack
import com.jongh0.bassshot.ui.theme.SubtleTitanium

@Composable
fun PermissionGate(
    onPermissionsResult: (hasCamera: Boolean, hasLocation: Boolean) -> Unit = { _, _ -> },
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.CAMERA))
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        val locationGranted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
            hasLocationPermission

        hasCameraPermission = cameraGranted
        hasLocationPermission = locationGranted
        onPermissionsResult(cameraGranted, locationGranted)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            val list = mutableListOf<String>()
            if (!hasCameraPermission) list.add(Manifest.permission.CAMERA)
            if (!hasLocationPermission) {
                list.add(Manifest.permission.ACCESS_FINE_LOCATION)
                list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            launcher.launch(list.toTypedArray())
        } else {
            onPermissionsResult(true, true)
        }
    }

    if (hasCameraPermission) {
        content()
    } else {
        // 권한 요청 대기 화면
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AnodizedCharcoal)
                    .border(1.dp, SubtleTitanium, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "카메라 및 위치 권한 필요",
                    color = ChampagneGold,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "배스 자동 계측 및 타임마크 촬영을 위해\n카메라 권한 승인이 반드시 필요합니다.",
                    color = MutedSilver,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = ObsidianBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "권한 허용하기",
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                }
            }
        }
    }
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
