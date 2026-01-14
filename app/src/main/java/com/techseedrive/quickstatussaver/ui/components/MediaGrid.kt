package com.techseedrive.quickstatussaver.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
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
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.techseedrive.quickstatussaver.model.StatusMedia
import androidx.compose.runtime.CompositionLocalProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.decode.VideoFrameDecoder

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
                    isScrolling = isScrolling,
                    allItems = items
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
    isScrolling: Boolean = false,
    allItems: List<StatusMedia> = emptyList()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val onClick = remember(media.uri, fromSavedStatus, allItems) {
        {
            // Cache the media list for fullscreen navigation
            FullScreenMediaCache.setMediaList(allItems)

            val encodedUri = Uri.encode(media.uri.toString())
            navController.navigate(
                "fullScreen/$encodedUri/${media.isVideo}/${media.displayName}/${media.lastModified}/$fromSavedStatus"
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Use Coil AsyncImage for efficient image/video loading
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.uri)
                .apply {
                    if (media.isVideo) {
                        // Use VideoFrameDecoder for video thumbnails
                        decoderFactory { result, options, _ ->
                            VideoFrameDecoder(result.source, options)
                        }
                    }
                }
                .crossfade(150)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .size(400, 400)
                .build(),
            contentDescription = media.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = null,
            error = null
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
    }
}

