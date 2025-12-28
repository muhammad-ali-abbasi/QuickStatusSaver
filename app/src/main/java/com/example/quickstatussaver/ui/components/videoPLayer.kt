package com.example.quickstatussaver.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.quickstatussaver.R
import com.example.quickstatussaver.model.StatusMedia

@Composable
fun VideoPlayer(videoUri: Uri, fullScreen: Boolean = false) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                // Enable hardware acceleration for smoother playback
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // Set video URI
                setVideoURI(videoUri)

                // Setup media controller
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)

                // Optimize buffering and playback
                setOnPreparedListener { mp ->
                    // Enable looping for better UX
                    mp.isLooping = false

                    // Set audio attributes for better performance
                    mp.setVolume(1.0f, 1.0f)

                    // Auto-start in fullscreen mode
                    if (fullScreen) {
                        start()
                    } else {
                        pause() // Stay paused in grid view
                    }
                }

                // Error handling
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("VideoPlayer", "Error: $what, $extra")
                    false
                }
            }
        },
        modifier = if (fullScreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxWidth().height(180.dp)
        }
    )
}
