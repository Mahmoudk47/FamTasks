package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * FamTasks Brand Logo Composable
 * Minimal, modern, clean, and easily recognizable at all sizes.
 */
@Composable
fun FamTasksLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 64
) {
    Image(
        painter = painterResource(id = R.drawable.ic_famtasks_logo),
        contentDescription = "FamTasks Logo",
        modifier = modifier.size(sizeDp.dp)
    )
}
