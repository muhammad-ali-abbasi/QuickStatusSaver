package com.example.quickstatussaver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.quickstatussaver.ui.theme.QuickStatusSaverTheme
import com.example.quickstatussaver.utils.PermissionManager

@OptIn(ExperimentalFoundationApi::class)  // Opt-in for the experimental pager API
@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }

    // ✅ Call callback inside LaunchedEffect instead of directly
    LaunchedEffect(Unit) {
        if (permissionManager.isPermissionGranted()) {
            navController.navigate("whatsapp") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Show intro screens until permissions are granted
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
        when (pageIndex) {
            0 -> WelcomeScreen()
            1 -> PermissionsScreen(
                onPermissionGranted = {
                    permissionManager.setPermissionGranted(true)
                    navController.navigate("whatsapp") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun WelcomeScreen() {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.welcome_animation))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Lottie Animation
            LottieAnimation(composition = composition, modifier = Modifier.size(200.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Welcome to QuickStatusSaver", style = TextStyle(fontSize = 24.sp))

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Save and manage your status quickly and efficiently.")
        }
    }
}

@Composable
fun PermissionsScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    var showPermissionDenied by remember { mutableStateOf(false) }
    var shouldShowRationale by remember { mutableStateOf(false) }

    // Define the permissions based on Android version
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsResult ->
            val allGranted = permissionsResult.values.all { it }
            if (allGranted) {
                onPermissionGranted()
            } else {
                showPermissionDenied = true
                // Check if we should show rationale for any permission
                shouldShowRationale = permissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        permission
                    )
                }
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Permissions", style = TextStyle(fontSize = 24.sp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (showPermissionDenied && shouldShowRationale) {
                    "Permissions are required to save statuses to your device. " +
                            "Please grant them to continue."
                } else if (showPermissionDenied) {
                    "Permissions denied. Please go to app settings to grant them."
                } else {
                    "We need the following permissions to proceed:"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                permissionsLauncher.launch(permissions)
            }) {
                Text(text = if (showPermissionDenied) "Retry" else "Grant Permissions")
            }

            if (showPermissionDenied && !shouldShowRationale) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intent)
                    }
                ) {
                    Text(text = "Open Settings")
                }
            }
        }
    }
}

