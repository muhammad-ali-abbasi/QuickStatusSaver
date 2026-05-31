package com.techseedrive.quickstatussaver.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.techseedrive.quickstatussaver.model.StatusMedia
import kotlin.math.abs

/**
 * Custom FlingBehavior for smooth, controlled scrolling.
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
        frictionMultiplier = 0.65f,
        absVelocityThreshold = 0.3f
    )
    return remember { SmoothFlingBehavior(decaySpec, 0.65f) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<StatusMedia>,
    fromSavedStatus: Boolean = false,
    navController: NavHostController,
    deleteItem: ((StatusMedia) -> Unit)? = null,
    // ── Multi-select state (hoisted from parent) ──
    selectedItems: Set<Uri> = emptySet(),
    isSelectionMode: Boolean = false,
    onToggleSelection: (StatusMedia) -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val smoothFlingBehavior = rememberSmoothFlingBehavior()
    val isScrolling = gridState.isScrollInProgress

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            // Add bottom padding so the last row is never hidden behind the action bar
            contentPadding = PaddingValues(
                start = 2.dp,
                top = 2.dp,
                end = 2.dp,
                bottom = if (isSelectionMode) 80.dp else 2.dp
            ),
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
                    allItems = items,
                    isSelected = selectedItems.contains(media.uri),
                    isSelectionMode = isSelectionMode,
                    onToggleSelection = onToggleSelection,
                    onSelectionModeChange = onSelectionModeChange
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    media: StatusMedia,
    context: Context,
    navController: NavHostController,
    fromSavedStatus: Boolean,
    deleteItem: ((StatusMedia) -> Unit)?,
    isScrolling: Boolean = false,
    allItems: List<StatusMedia> = emptyList(),
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelection: (StatusMedia) -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    val openFullscreen = remember(media.uri, fromSavedStatus, allItems) {
        {
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
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection(media)
                    } else {
                        openFullscreen()
                    }
                },
                onLongClick = {
                    // Enter selection mode on long press and select this item
                    if (!isSelectionMode) {
                        onSelectionModeChange(true)
                    }
                    onToggleSelection(media)
                }
            )
    ) {
        // ── Thumbnail (image or video frame) ──
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.uri)
                .apply {
                    if (media.isVideo) {
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

        // ── Video play icon (hidden during selection mode) ──
        if (media.isVideo && !isSelectionMode) {
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

        // ── Selection overlay ──
        if (isSelectionMode || isSelected) {
            // Dark tint when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            // Checkbox icon in the top-right corner
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(22.dp)
                        .then(
                            // White circle backing for the filled check icon so it pops on any BG
                            if (isSelected) Modifier.background(Color.White, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
