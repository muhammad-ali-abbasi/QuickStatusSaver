package com.example.quickstatussaver.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.quickstatussaver.model.StatusMedia
import androidx.compose.ui.layout.ContentScale
import com.example.quickstatussaver.utils.AppUtils.deleteMedia
import com.example.quickstatussaver.utils.AppUtils.saveMedia
import com.example.quickstatussaver.utils.AppUtils.shareMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenComponent(
    mediaUri: Uri,
    isVideo: Boolean,
    displayName: String,
    lastModified: Long,
    fromSavedStatus: Boolean,
    navController: NavController
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val media = StatusMedia(
        uri = mediaUri,
        isVideo = isVideo,
        displayName = displayName,
        lastModified = lastModified
    )

    Box( modifier = Modifier
            .fillMaxSize()
        .background(color = backgroundColor)) {

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Full Screen View") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isVideo) {
                    // VideoPlayer with optimizations
                    VideoPlayer(videoUri = mediaUri, fullScreen = true)
                } else {
                    // Optimized image loading with proper sizing
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mediaUri)
                            .crossfade(200)
                            .memoryCacheKey(mediaUri.toString())
                            .diskCacheKey(mediaUri.toString())
                            .build(),
                        contentDescription = "Full Screen Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Action icons at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x88000000), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!fromSavedStatus) {
                IconButton(onClick = { saveMedia(context, media) }) {
                    Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                }
            }
            IconButton(onClick = { shareMedia(context,media) }) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
            }
            if (fromSavedStatus) {
                IconButton(onClick = {
                    deleteMedia(context,media)
                    navController.popBackStack()
                }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}
