package com.techseedrive.quickstatussaver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.techseedrive.quickstatussaver.utils.PermissionManager
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            when (pageIndex) {
                0 -> WelcomeScreen(
                    onNextClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
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

        // Page indicators at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onNextClick: () -> Unit) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.welcome_animation)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Lottie Animation
            LottieAnimation(composition = composition, modifier = Modifier.size(200.dp))

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to Quick Status Saver",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Save and manage your WhatsApp statuses quickly and efficiently.",
                style = TextStyle(fontSize = 16.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Next button
            Button(
                onClick = onNextClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text("Get Started")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Swipe hint
            Text(
                text = "or swipe to continue →",
                style = TextStyle(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(60.dp)) // Space for page indicators
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

