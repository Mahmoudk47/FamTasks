package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R

val AVATAR_TEMPLATES = listOf(
    "avatar_bear" to R.drawable.avatar_bear,
    "avatar_fox" to R.drawable.avatar_fox,
    "avatar_panda" to R.drawable.avatar_panda,
    "avatar_lion" to R.drawable.avatar_lion,
    "avatar_owl" to R.drawable.avatar_owl,
    "avatar_robot" to R.drawable.avatar_robot,
    "avatar_superhero" to R.drawable.avatar_superhero,
    "avatar_star" to R.drawable.avatar_star
)

fun getDrawableIdForTemplate(templateName: String): Int? {
    return AVATAR_TEMPLATES.find { it.first == templateName }?.second
}

@Composable
fun UserAvatar(
    avatarTemplate: String,
    avatarUri: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 48
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(avatarUri))
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val resId = getDrawableIdForTemplate(avatarTemplate)
            if (resId != null) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Avatar Template",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((sizeDp * 0.6).dp)
                )
            }
        }
    }
}

@Composable
fun ProfilePictureSection(
    selectedTemplate: String,
    selectedUri: String,
    onTemplateSelected: (String) -> Unit,
    onUriSelected: (String) -> Unit,
    onRemovePicture: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTemplateDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                onUriSelected(uri.toString())
            }
        }
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Avatar Preview Circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .testTag("avatar_preview_box"),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                avatarTemplate = selectedTemplate,
                avatarUri = selectedUri,
                sizeDp = 96
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Profile Picture (Optional)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons: Choose Template / Upload Photo
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showTemplateDialog = true },
                modifier = Modifier
                    .height(36.dp)
                    .testTag("choose_avatar_template_button")
            ) {
                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Choose Template", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .height(36.dp)
                    .testTag("upload_photo_button")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Photo", fontSize = 12.sp)
            }

            if (selectedTemplate.isNotBlank() || selectedUri.isNotBlank()) {
                TextButton(
                    onClick = onRemovePicture,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showTemplateDialog) {
        AvatarTemplateDialog(
            selectedTemplate = selectedTemplate,
            onSelect = { templateName ->
                onTemplateSelected(templateName)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}

@Composable
fun AvatarTemplateDialog(
    selectedTemplate: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Avatar Template",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Pick one of the illustrated avatar templates below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(AVATAR_TEMPLATES) { (name, resId) ->
                        val isSelected = selectedTemplate == name
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onSelect(name) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
