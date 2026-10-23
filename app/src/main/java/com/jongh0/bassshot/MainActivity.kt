package com.jongh0.bassshot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jongh0.bassshot.ui.camera.BassShotScreen
import com.jongh0.bassshot.ui.camera.BassShotViewModel
import com.jongh0.bassshot.ui.permission.PermissionGate
import com.jongh0.bassshot.ui.theme.BassShotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BassShotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BassShotTheme {
                var hasLocationPermission by remember { mutableStateOf(false) }

                PermissionGate(
                    onPermissionsResult = { _, locationGranted ->
                        hasLocationPermission = locationGranted
                        viewModel.refreshEnvironmentData(locationGranted)
                    }
                ) {
                    BassShotScreen(
                        viewModel = viewModel,
                        hasLocationPermission = hasLocationPermission
                    )
                }
            }
        }
    }
}
