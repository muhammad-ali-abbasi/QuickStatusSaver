package com.techseedrive.quickstatussaver.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.techseedrive.quickstatussaver.model.StatusMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Custom FlingBehavior for smooth, controlled scrolling
 * Simplified for better performance
 */
class SmoothFlingBehavior(
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val velocityMultiplier: Float = 0.6f
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val reducedVelocity = initialVelocity * velocityMultiplier

        var lastValue = 0f
        AnimationState(
            initialValue = 0f,
            initialVelocity = reducedVelocity,
        ).animateDecay(decayAnimationSpec) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            if (abs(delta - consumed) > 1.0f) this.cancelAnimation()
        }
        return 0f
    }
}

@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decaySpec = exponentialDecay<Float>(
        frictionMultiplier = 0.65f, // Optimized for smooth, natural feel
        absVelocityThreshold = 0.3f // Smoother stop
    )
    return remember { SmoothFlingBehavior(decaySpec, 0.65f) } // Slightly faster
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<StatusMedia>,
    fromSavedStatus: Boolean = false,
    navController: NavHostController,
    deleteItem: ((StatusMedia) -> Unit)? = null
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val smoothFlingBehavior = rememberSmoothFlingBehavior()

    // Detect if user is actively scrolling - pause loading during fast scroll
    val isScrolling = gridState.isScrollInProgress

    // Disable overscroll bounce effect for smooth, controlled scrolling
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(2.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            state = gridState,
            flingBehavior = smoothFlingBehavior
        ) {
            items(
                items = items,
                key = { it.uri.toString() }
            ) { media ->
                MediaItemCard(
                    media = media,
                    context = context,
                    navController = navController,
                    fromSavedStatus = fromSavedStatus,
                    deleteItem = deleteItem,
                    isScrolling = isScrolling
                )
            }
        }
    }
}

@Composable
fun MediaItemCard(
    media: StatusMedia,
    context: Context,
    navController: NavHostController,
    fromSavedStatus: Boolean,
    deleteItem: ((StatusMedia) -> Unit)?,
    isScrolling: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val onClick = remember(media.uri, fromSavedStatus) {
        {
            val encodedUri = Uri.encode(media.uri.toString())
            navController.navigate(
                "fullScreen/$encodedUri/${media.isVideo}/${media.displayName}/${media.lastModified}/$fromSavedStatus"
            )
        }
    }

    Card(
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                // Load thumbnail for both images and videos
                val thumbnail = rememberMediaThumbnailSmart(
                    context = context,
                    uri = media.uri,
                    isVideo = media.isVideo,
                    isScrolling = isScrolling
                )

                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = media.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Show play icon overlay for videos
                    if (media.isVideo) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                } else {
                    // Placeholder while loading
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

//            MediaActions(
//                media = media,
//                showDelete = fromSavedStatus,
//                showSave = !fromSavedStatus,
//                deleteItem = deleteItem
//            )
        }
    }
}

@Composable
fun rememberMediaThumbnailSmart(
    context: Context,
    uri: Uri,
    isVideo: Boolean = false,
    isScrolling: Boolean
): Bitmap? {
    var thumbnailState by remember(uri) { mutableStateOf<Bitmap?>(null) }

    // Always check cache first
    LaunchedEffect(uri) {
        val cached = ThumbnailCache.get(uri)
        if (cached != null) {
            thumbnailState = cached
            return@LaunchedEffect
        }
    }

    // Only load if not scrolling - with delay to prevent lag
    LaunchedEffect(uri, isScrolling) {
        if (isScrolling) return@LaunchedEffect // Skip loading during scroll

        // Wait after scroll stops before loading for buttery smooth performance
        kotlinx.coroutines.delay(150)

        val cached = ThumbnailCache.get(uri)
        if (cached != null) {
            thumbnailState = cached
            return@LaunchedEffect
        }

        // Load in background
        withContext(Dispatchers.IO) {
            try {
                val thumb = if (isVideo) {
                    // Extract frame from video using MediaMetadataRetriever
                    Log.d("VideoThumbnail", "Extracting frame from video: $uri")
                    val retriever = MediaMetadataRetriever()
                    var result: Bitmap? = null
                    try {
                        retriever.setDataSource(context, uri)
                        // Get frame at 1 second (1000000 microseconds) for better thumbnail
                        val frame = retriever.getFrameAtTime(
                            1000000,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        if (frame != null) {
                            result = Bitmap.createScaledBitmap(frame, 400, 400, true)
                            Log.d("VideoThumbnail", "Successfully extracted video frame")
                        } else {
                            Log.e("VideoThumbnail", "getFrameAtTime returned null")
                        }
                    } catch (e: Exception) {
                        Log.e("VideoThumbnail", "Error extracting video frame: ${e.message}", e)
                    } finally {
                        try {
                            retriever.release()
                        } catch (e: Exception) {
                            Log.e("VideoThumbnail", "Error releasing retriever", e)
                        }
                    }
                    result
                } else {
                    // For images
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(
                            uri,
                            android.util.Size(400, 400),
                            null
                        )
                    } else {
                        val options = android.graphics.BitmapFactory.Options().apply {
                            inJustDecodeBounds = false
                            inSampleSize = 2
                            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                        }
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)?.let {
                                Bitmap.createScaledBitmap(it, 400, 400, true)
                            }
                        }
                    }
                }

                if (thumb != null) {
                    ThumbnailCache.put(uri, thumb)
                    thumbnailState = thumb
                    Log.d("Thumbnail", "Cached thumbnail for $uri")
                } else {
                    Log.e("Thumbnail", "Failed to load thumbnail (null) for $uri, isVideo=$isVideo")
                }
            } catch (e: Exception) {
                Log.e("Thumbnail", "Error loading thumbnail for $uri, isVideo=$isVideo", e)
            }
        }
    }

    return thumbnailState
}

@Composable
fun rememberMediaThumbnail(context: Context, uri: Uri, isVideo: Boolean): Bitmap? {
    var thumbnailState by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        // Check cache first - return immediately if cached
        val cached = ThumbnailCache.get(uri)
        if (cached != null) {
            thumbnailState = cached
            return@LaunchedEffect
        }

        // Load in background using Android's fast thumbnail API
        withContext(Dispatchers.IO) {
            try {
                val thumb = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Use Android's built-in thumbnail API (FASTEST - same as gallery app)
                    context.contentResolver.loadThumbnail(
                        uri,
                        android.util.Size(400, 400), // Increased from 100x100 for better quality
                        null
                    )
                } else {
                    // Fallback for older Android versions
                    if (isVideo) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val raw = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        retriever.release()
                        raw?.let { Bitmap.createScaledBitmap(it, 400, 400, true) } // true = use filtering
                    } else {
                        val options = android.graphics.BitmapFactory.Options().apply {
                            inJustDecodeBounds = false
                            inSampleSize = 2 // Reduced from 8 for better quality
                            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 // Better quality
                        }
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)?.let {
                                Bitmap.createScaledBitmap(it, 400, 400, true) // true = use filtering
                            }
                        }
                    }
                }

                thumb?.let {
                    ThumbnailCache.put(uri, it)
                    thumbnailState = it
                }
            } catch (e: Exception) {
                Log.e("Thumbnail", "Error loading thumbnail for $uri", e)
            }
        }
    }

    return thumbnailState
}

object ThumbnailCache {
    // Use 1/3 of available memory for thumbnail cache - more cache = smoother scrolling
    private val cacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 3).toInt()
    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun get(uri: Uri): Bitmap? = lruCache.get(uri.toString())

    fun put(uri: Uri, bitmap: Bitmap) {
        if (get(uri) == null) {
            lruCache.put(uri.toString(), bitmap)
        }
    }

    fun clear() {
        lruCache.evictAll()
    }
}