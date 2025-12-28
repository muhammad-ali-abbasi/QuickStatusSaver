package com.techseedrive.quickstatussaver.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.techseedrive.quickstatussaver.model.StatusMedia
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import com.techseedrive.quickstatussaver.utils.AppUtils.deleteMedia
import com.techseedrive.quickstatussaver.utils.AppUtils.repostMedia
import com.techseedrive.quickstatussaver.utils.AppUtils.saveMedia
import com.techseedrive.quickstatussaver.utils.AppUtils.shareMedia


@Composable
fun MediaActions(media: StatusMedia,showShare:Boolean=true,showSave:Boolean=true,showRepost:Boolean=true,showDelete:Boolean=true,deleteItem: ((StatusMedia) -> Unit)? = null) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if(showShare){
        ActionIconButton(
            icon = Icons.Default.Share,
            description = "Share",
            onClick = { shareMedia(context, media) }
        )
        }
        if(showSave) {
            ActionIconButton(
                icon = Icons.Default.Save,
                description = "Save",
                onClick = { saveMedia(context, media) }
            )
        }
        if(showRepost){
        ActionIconButton(
            icon = Icons.Default.CropRotate,
            description = "Repost",
            onClick = { repostMedia(context, media) }
        )
            }

        if (showDelete) {
            ActionIconButton(
                icon = Icons.Default.Delete,
                description = "Delete",
                onClick = {
                    deleteMedia(context, media)
                    if (deleteItem != null) {
                        deleteItem(media)
                    }
                }
            )
        }
    }
}

@Composable
fun ActionIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(45.dp)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

