package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamTasksRepository
import com.example.localization.AppLanguage
import com.example.localization.Strings
import com.example.model.Family
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.ProfilePictureSection
import com.example.ui.components.UserAvatar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentUser: User,
    currentFamily: Family?,
    currentLanguage: AppLanguage,
    currentThemeMode: String,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSignOut: () -> Unit,
    repository: FamTasksRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val codeCopiedMsg = Strings.get("code_copied")
    val profileUpdatedMsg = Strings.get("profile_updated")

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var isSavingProfile by remember { mutableStateOf(false) }
    var profileErrorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SnackbarHost(hostState = snackbarHostState)

        // User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        avatarTemplate = currentUser.avatarTemplate,
                        avatarUri = currentUser.avatarUri,
                        sizeDp = 56
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.displayName.ifBlank { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (currentUser.role) {
                                UserRole.SUPER_ADMIN.value -> Strings.get("role_super_admin")
                                UserRole.FAMILY_ADMIN.value -> Strings.get("role_family_admin")
                                else -> Strings.get("role_family_member")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${currentUser.pointsBalance}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Edit Profile Button
                OutlinedButton(
                    onClick = {
                        profileErrorMessage = null
                        showEditProfileDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("edit_profile"), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Family Info Card
        if (currentFamily != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FamilyRestroom, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentFamily.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Strings.get("join_code"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentFamily.joinCode,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Family Code", currentFamily.joinCode)
                                clipboard.setPrimaryClip(clip)
                                scope.launch {
                                    snackbarHostState.showSnackbar(codeCopiedMsg)
                                }
                            },
                            modifier = Modifier.testTag("copy_join_code_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = Strings.get("copy_code"))
                        }
                    }
                }
            }
        }

        // Language Setting (ENGLISH & ARABIC ONLY)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("language_setting"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = currentLanguage == AppLanguage.ENGLISH,
                        onClick = {
                            onLanguageChange(AppLanguage.ENGLISH)
                            scope.launch {
                                repository.updateUserPreferences(currentUser.userId, language = "en")
                            }
                        },
                        label = { Text("English") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("language_en_button")
                    )

                    FilterChip(
                        selected = currentLanguage == AppLanguage.ARABIC,
                        onClick = {
                            onLanguageChange(AppLanguage.ARABIC)
                            scope.launch {
                                repository.updateUserPreferences(currentUser.userId, language = "ar")
                            }
                        },
                        label = { Text("العربية (Arabic)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("language_ar_button")
                    )
                }
            }
        }

        // Theme Setting (System, Light, Dark)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("theme_setting"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentThemeMode == "system",
                        onClick = {
                            onThemeModeChange("system")
                            scope.launch { repository.updateUserPreferences(currentUser.userId, theme = "system") }
                        },
                        label = { Text(Strings.get("system_theme")) },
                        leadingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentThemeMode == "light",
                        onClick = {
                            onThemeModeChange("light")
                            scope.launch { repository.updateUserPreferences(currentUser.userId, theme = "light") }
                        },
                        label = { Text("Light") },
                        leadingIcon = { Icon(Icons.Default.LightMode, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentThemeMode == "dark",
                        onClick = {
                            onThemeModeChange("dark")
                            scope.launch { repository.updateUserPreferences(currentUser.userId, theme = "dark") }
                        },
                        label = { Text("Dark") },
                        leadingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Notification Preferences
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("notification_settings"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                NotificationToggleRow(
                    title = Strings.get("notify_new_tasks"),
                    checked = currentUser.notifyNewTasks,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateUserPreferences(currentUser.userId, notifyNewTasks = checked) }
                    }
                )

                NotificationToggleRow(
                    title = Strings.get("notify_task_changes"),
                    checked = currentUser.notifyTaskChanges,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateUserPreferences(currentUser.userId, notifyTaskChanges = checked) }
                    }
                )

                NotificationToggleRow(
                    title = Strings.get("notify_store_items"),
                    checked = currentUser.notifyStoreItems,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateUserPreferences(currentUser.userId, notifyStoreItems = checked) }
                    }
                )

                NotificationToggleRow(
                    title = Strings.get("notify_purchases"),
                    checked = currentUser.notifyPurchases,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateUserPreferences(currentUser.userId, notifyPurchases = checked) }
                    }
                )

                NotificationToggleRow(
                    title = Strings.get("notify_approvals"),
                    checked = currentUser.notifyApprovals,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateUserPreferences(currentUser.userId, notifyApprovals = checked) }
                    }
                )
            }
        }

        // Sign Out Button
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_out_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.get("sign_out"), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentUser = currentUser,
            isSaving = isSavingProfile,
            errorMessage = profileErrorMessage,
            onDismiss = {
                if (!isSavingProfile) {
                    showEditProfileDialog = false
                    profileErrorMessage = null
                }
            },
            onSave = { fName, sName, tmpl, uri ->
                scope.launch {
                    isSavingProfile = true
                    profileErrorMessage = null
                    val res = repository.updateUserProfile(
                        userId = currentUser.userId,
                        firstName = fName,
                        secondName = sName,
                        avatarTemplate = tmpl,
                        avatarUri = uri
                    )
                    isSavingProfile = false
                    if (res.isSuccess) {
                        showEditProfileDialog = false
                        snackbarHostState.showSnackbar(profileUpdatedMsg)
                    } else {
                        profileErrorMessage = res.exceptionOrNull()?.message ?: "Failed to update profile"
                    }
                }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentUser: User,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (firstName: String, secondName: String, avatarTemplate: String, avatarUri: String) -> Unit
) {
    var firstName by remember {
        mutableStateOf(
            currentUser.firstName.ifBlank {
                currentUser.displayName.substringBefore(" ")
            }
        )
    }
    var secondName by remember {
        mutableStateOf(
            currentUser.secondName.ifBlank {
                currentUser.displayName.substringAfter(" ", "")
            }
        )
    }
    var selectedTemplate by remember { mutableStateOf(currentUser.avatarTemplate) }
    var selectedUri by remember { mutableStateOf(currentUser.avatarUri) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val fillAllFieldsMsg = Strings.get("fill_all_fields")

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("edit_profile"), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Picture (Optional, Choose Template, Upload, or Clear/Remove)
                ProfilePictureSection(
                    selectedTemplate = selectedTemplate,
                    selectedUri = selectedUri,
                    onTemplateSelected = { tmpl ->
                        selectedTemplate = tmpl
                        selectedUri = ""
                    },
                    onUriSelected = { uri ->
                        selectedUri = uri
                        selectedTemplate = ""
                    },
                    onRemovePicture = {
                        selectedTemplate = ""
                        selectedUri = ""
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        validationError = null
                    },
                    label = { Text(Strings.get("first_name")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_first_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = secondName,
                    onValueChange = {
                        secondName = it
                        validationError = null
                    },
                    label = { Text(Strings.get("second_name")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_second_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val displayErr = validationError ?: errorMessage
                if (displayErr != null) {
                    Text(
                        text = displayErr,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedFirst = firstName.trim()
                    val trimmedSecond = secondName.trim()
                    if (trimmedFirst.isBlank() || trimmedSecond.isBlank()) {
                        validationError = fillAllFieldsMsg
                        return@Button
                    }
                    onSave(trimmedFirst, trimmedSecond, selectedTemplate, selectedUri)
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("save_profile_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(Strings.get("save_changes"), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(Strings.get("cancel"))
            }
        }
    )
}

@Composable
fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
