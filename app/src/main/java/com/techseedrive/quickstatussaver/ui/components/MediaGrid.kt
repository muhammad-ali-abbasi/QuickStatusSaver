package com.techseedrive.quickstatussaver.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.techseedrive.quickstatussaver.model.StatusMedia
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * Performance-Optimized MediaGrid.
 * We removed all custom fling restrictions to match Native Gallery velocity.
 * Image loading is optimized with lower precision and smaller dimensions for the grid.
 */

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
    val density = LocalDensity.current

    // ── Drag Selection state ──
    var dragStartItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf<Offset?>(null) }
    var containerHeight by remember { mutableStateOf(0f) }

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

    // Auto-scroll logic during drag
    LaunchedEffect(dragOffset, containerHeight) {
        val offset = dragOffset ?: return@LaunchedEffect
        if (containerHeight <= 0) return@LaunchedEffect
        val threshold = with(density) { 60.dp.toPx() }
        val scrollDelta = when {
            offset.y < threshold -> -25f * (1f - (offset.y / threshold).coerceIn(0f, 1f))
            offset.y > containerHeight - threshold -> 30f * (1f - ((containerHeight - offset.y) / threshold).coerceIn(0f, 1f))
            else -> 0f
        }
        if (scrollDelta != 0f) {
            while (dragOffset != null) {
                gridState.scroll { scrollBy(scrollDelta) }
                dragOffset?.let { currentOffset ->
                    val newIndex = getItemIndexAtOffset(currentOffset)
                    if (newIndex != null && newIndex != dragCurrentItemIndex) {
                        dragCurrentItemIndex = newIndex
                    }
                }
                delay(16)
            }
        }
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
                .onGloballyPositioned { containerHeight = it.size.height.toFloat() }
                .pointerInput(items) {
                    detectTapGestures(
                        onTap = { offset ->
                            val index = getItemIndexAtOffset(offset)
                            if (index != null) {
                                val media = items[index]
                                if (currentIsSelectionMode) {
                                    onToggleSelection(media)
                                } else {
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
                                dragOffset = offset
                                onToggleSelection(items[index])
                            }
                        },
                        onDrag = { change, _ ->
                            dragOffset = change.position
                            val index = getItemIndexAtOffset(change.position)
                            if (index != null && index != dragCurrentItemIndex) {
                                dragCurrentItemIndex = index
                            }
                        },
                        onDragEnd = {
                            dragStartItemIndex = null
                            dragCurrentItemIndex = null
                            dragOffset = null
                        },
                        onDragCancel = {
                            dragStartItemIndex = null
                            dragCurrentItemIndex = null
                            dragOffset = null
                        }
                    )
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 1.dp,
                    top = 1.dp,
                    end = 1.dp,
                    bottom = if (isSelectionMode) 80.dp else 1.dp
                ),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                state = gridState
                // Removed SmoothFlingBehavior completely to restore Native speed
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.uri.toString() }
                ) { _, media ->
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
            .aspectRatio(1f) // Ensures items have a consistent size for better scroll performance
            .clip(RoundedCornerShape(2.dp))
            .background(Color.LightGray.copy(alpha = 0.1f)) // Placeholder color
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
                .size(250) // Reduced resolution for faster loading in 3-column grid
                .precision(Precision.INEXACT)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low
        )

        if (media.isVideo && !isSelectionMode) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    tint = Color.White.copy(alpha = 0.8f),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
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
                        .padding(4.dp)
                        .size(20.dp)
                        .then(
                            if (isSelected) Modifier.background(Color.White, CircleShape)
                            else Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary 
                              else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
