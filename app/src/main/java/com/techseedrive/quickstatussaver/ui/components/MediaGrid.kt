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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntRect
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.techseedrive.quickstatussaver.model.StatusMedia
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
    selectedItems: Set<Uri> = emptySet(),
    isSelectionMode: Boolean = false,
    onToggleSelection: (StatusMedia) -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    onBulkSelect: (List<Uri>) -> Unit = {}
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val smoothFlingBehavior = rememberSmoothFlingBehavior()

    // ── Drag Selection State ──────────────────────────────────────────────
    var dragStartItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentItemIndex by remember { mutableStateOf<Int?>(null) }
    
    val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
    val currentOnSelectionModeChange by rememberUpdatedState(onSelectionModeChange)

    fun getItemIndexAtOffset(offset: Offset): Int? {
        val layoutInfo = gridState.layoutInfo
        val itemsInfo = layoutInfo.visibleItemsInfo
        if (itemsInfo.isEmpty()) return null
        
        val item = itemsInfo.find { itemInfo ->
            val x = itemInfo.offset.x.toFloat()
            val y = itemInfo.offset.y.toFloat()
            val w = itemInfo.size.width.toFloat()
            val h = itemInfo.size.height.toFloat()
            
            offset.x >= x && offset.x <= (x + w) && 
            offset.y >= y && offset.y <= (y + h)
        }
        return item?.index
    }

    LaunchedEffect(dragStartItemIndex, dragCurrentItemIndex) {
        val start = dragStartItemIndex
        val end = dragCurrentItemIndex
        if (start != null && end != null) {
            val startIndex = min(start, end)
            val endIndex = max(start, end)
            val rangeUris = mutableListOf<Uri>()
            for (i in max(0, startIndex)..min(items.lastIndex, endIndex)) {
                rangeUris.add(items[i].uri)
            }
            if (rangeUris.isNotEmpty()) onBulkSelect(rangeUris)
        }
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Use a single pointerInput to handle both Taps and LongPress + Drag
                // This prevents child clickables from interfering with selection logic
                .pointerInput(items) {
                    detectTapGestures(
                        onTap = { offset ->
                            val index = getItemIndexAtOffset(offset)
                            if (index != null) {
                                val media = items[index]
                                if (currentIsSelectionMode) {
                                    onToggleSelection(media)
                                } else {
                                    // Open Full Screen
                                    FullScreenMediaCache.setMediaList(items)
                                    val encodedUri = Uri.encode(media.uri.toString())
                                    navController.navigate(
                                        "fullScreen/$encodedUri/${media.isVideo}/${media.displayName}/${media.lastModified}/$fromSavedStatus"
                                    )
                                }
                            }
                        }
                    )
                }
                .pointerInput(items) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val index = getItemIndexAtOffset(offset)
                            if (index != null) {
                                if (!currentIsSelectionMode) {
                                    currentOnSelectionModeChange(true)
                                }
                                dragStartItemIndex = index
                                dragCurrentItemIndex = index
                                // Toggle the first item
                                onToggleSelection(items[index])
                            }
                        },
                        onDrag = { change, _ ->
                            val index = getItemIndexAtOffset(change.position)
                            if (index != null && index != dragCurrentItemIndex) {
                                dragCurrentItemIndex = index
                            }
                        },
                        onDragEnd = {
                            dragStartItemIndex = null
                            dragCurrentItemIndex = null
                        },
                        onDragCancel = {
                            dragStartItemIndex = null
                            dragCurrentItemIndex = null
                        }
                    )
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
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
                flingBehavior = smoothFlingBehavior,
                userScrollEnabled = dragStartItemIndex == null
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.uri.toString() }
                ) { index, media ->
                    MediaItemCard(
                        media = media,
                        context = context,
                        isSelected = selectedItems.contains(media.uri),
                        isSelectionMode = isSelectionMode
                    )
                }
            }
        }
    }
}

@Composable
fun MediaItemCard(
    media: StatusMedia,
    context: Context,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(4.dp))
            // No clickables here - all handled by the parent grid for perfect sync
    ) {
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
            contentScale = ContentScale.Crop
        )

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

        if (isSelectionMode || isSelected) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(22.dp)
                        .then(
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
