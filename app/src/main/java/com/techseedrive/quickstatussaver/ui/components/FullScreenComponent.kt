package com.techseedrive.quickstatussaver.ui.components

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.techseedrive.quickstatussaver.model.StatusMedia
import androidx.compose.ui.layout.ContentScale
import com.techseedrive.quickstatussaver.utils.AppUtils.deleteMedia
import com.techseedrive.quickstatussaver.utils.AppUtils.saveMedia
import com.techseedrive.quickstatussaver.utils.AppUtils.shareMedia
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

// Cache to store media list for fullscreen navigation
object FullScreenMediaCache {
    private var mediaList: List<StatusMedia> = emptyList()

    fun setMediaList(list: List<StatusMedia>) {
        mediaList = list
    }

    fun getMediaList(): List<StatusMedia> = mediaList

    fun findIndexByUri(uri: Uri): Int {
        return mediaList.indexOfFirst { it.uri == uri }.takeIf { it >= 0 } ?: 0
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
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

    // Get the media list from cache
    val mediaList = remember { FullScreenMediaCache.getMediaList() }
    val initialIndex = remember { FullScreenMediaCache.findIndexByUri(mediaUri) }

    // Use HorizontalPager for swipe navigation
    val pagerState = rememberPagerState(initialPage = initialIndex)
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = backgroundColor)) {

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        if (mediaList.isNotEmpty() && pagerState.currentPage < mediaList.size) {
                            "${pagerState.currentPage + 1} / ${mediaList.size}"
                        } else {
                            "Full Screen View"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
            )

            if (mediaList.isNotEmpty()) {
                // HorizontalPager for swipe navigation
                HorizontalPager(
                    count = mediaList.size,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val media = mediaList[page]

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (media.isVideo) {
                            // VideoPlayer with optimizations
                            VideoPlayer(videoUri = media.uri, fullScreen = true)
                        } else {
                            // Zoomable image with page change reset
                            ZoomableImage(
                                imageUri = media.uri,
                                context = context,
                                currentPage = pagerState.currentPage
                            )
                        }
                    }
                }
            } else {
                // Fallback to single media display if cache is empty
                val media = StatusMedia(
                    uri = mediaUri,
                    isVideo = isVideo,
                    displayName = displayName,
                    lastModified = lastModified
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        VideoPlayer(videoUri = mediaUri, fullScreen = true)
                    } else {
                        ZoomableImage(mediaUri, context, currentPage = 0)
                    }
                }
            }
        }

        // Action icons at the bottom - for current media
        if (mediaList.isNotEmpty() && pagerState.currentPage < mediaList.size) {
            val currentMedia = mediaList[pagerState.currentPage]
            FullScreenActions(
                media = currentMedia,
                fromSavedStatus = fromSavedStatus,
                navController = navController,
                context = context
            )
        }
    }
}

@Composable
fun ZoomableImage(imageUri: Uri, context: Context, currentPage: Int) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isZoomEnabled by remember { mutableStateOf(false) }

    // Reset zoom when page changes
    LaunchedEffect(currentPage) {
        scale = 1f
        offset = Offset.Zero
        isZoomEnabled = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(200)
                .memoryCacheKey(imageUri.toString())
                .diskCacheKey(imageUri.toString())
                .build(),
            contentDescription = "Zoomable Full Screen Image",
            modifier = Modifier
                .fillMaxSize()
                // Double-tap to enable zoom mode
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                // Reset zoom
                                scale = 1f
                                offset = Offset.Zero
                                isZoomEnabled = false
                            } else {
                                // Zoom in to 2x
                                scale = 2f
                                isZoomEnabled = true
                            }
                        }
                    )
                }
                // Pinch zoom (only when enabled or scale > 1f)
                .then(
                    if (isZoomEnabled || scale > 1f) {
                        Modifier.pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                // Allow panning when zoomed
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = Offset.Zero
                                    isZoomEnabled = false
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun FullScreenActions(
    media: StatusMedia,
    fromSavedStatus: Boolean,
    navController: NavController,
    context: Context
) {
    // Permission launcher for delete requests on Android 10+
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Permission granted, try deleting again
            val intentSender = deleteMedia(context, media)
            if (intentSender == null) {
                // Successfully deleted, navigate back
                navController.popBackStack()
            }
        }
    }

    val handleDelete: () -> Unit = {
        val intentSender = deleteMedia(context, media)
        if (intentSender != null) {
            // Need user permission - show system dialog
            val request = IntentSenderRequest.Builder(intentSender).build()
            deletePermissionLauncher.launch(request)
        } else {
            // Successfully deleted or error, navigate back
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.BottomEnd)
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
        IconButton(onClick = { shareMedia(context, media) }) {
            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
        }
        if (fromSavedStatus) {
            IconButton(onClick = handleDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
