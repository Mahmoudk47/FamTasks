package com.example.ui.superadmin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamTasksRepository
import com.example.localization.Strings
import com.example.model.AppNotification
import com.example.model.Family
import com.example.model.JoinRequest
import com.example.model.Purchase
import com.example.model.StoreItem
import com.example.model.Task
import com.example.model.User
import com.example.ui.approvals.ApprovalsScreen
import com.example.ui.components.UserAvatar
import com.example.ui.notifications.NotificationCard
import com.example.ui.store.StoreItemCard
import com.example.ui.store.StoreItemDialog
import com.example.ui.tasks.TaskCard
import com.example.ui.tasks.TaskEditDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboard(
    families: List<Family>,
    allUsers: List<User>,
    allTasks: List<Task>,
    allStoreItems: List<StoreItem>,
    allPurchases: List<Purchase>,
    allJoinRequests: List<JoinRequest>,
    allNotifications: List<AppNotification>,
    repository: FamTasksRepository,
    onExitDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    var userToEditPoints by remember { mutableStateOf<User?>(null) }
    var familyToChangeAdmin by remember { mutableStateOf<Family?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<StoreItem?>(null) }
    var showStoreDialog by remember { mutableStateOf(false) }

    val superAdminUser = remember {
        User(
            userId = "super_admin_fixed_id",
            email = "admin@admin.com",
            displayName = "Super Admin / Manager",
            role = "super_admin"
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = Strings.get("super_admin_dashboard"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Full System Access (admin@admin.com)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onExitDashboard,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(Strings.get("sign_out"), fontSize = 12.sp)
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 16.dp
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Families (${families.size})") },
                            icon = { Icon(Icons.Default.FamilyRestroom, null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Users & Points (${allUsers.size})") },
                            icon = { Icon(Icons.Default.Group, null) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Tasks (${allTasks.size})") },
                            icon = { Icon(Icons.Default.Assignment, null) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Store (${allStoreItems.size})") },
                            icon = { Icon(Icons.Default.ShoppingCart, null) }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("Approvals") }
                        )
                        Tab(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            text = { Text("Notifications (${allNotifications.size})") },
                            icon = { Icon(Icons.Default.Notifications, null) }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                // 0: Families Management
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(families, key = { it.familyId }) { family ->
                            FamilyManagementCard(
                                family = family,
                                allUsers = allUsers,
                                onChangeAdmin = { familyToChangeAdmin = family },
                                onDelete = {
                                    scope.launch { repository.deleteFamily(family.familyId) }
                                }
                            )
                        }
                    }
                }

                // 1: Users & Points Management
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allUsers, key = { it.userId }) { user ->
                            UserPointsCard(
                                user = user,
                                onEditPoints = { userToEditPoints = user }
                            )
                        }
                    }
                }

                // 2: All Tasks Management
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allTasks, key = { it.taskId }) { task ->
                            TaskCard(
                                task = task,
                                currentUser = superAdminUser,
                                isAdmin = true,
                                onComplete = {
                                    scope.launch {
                                        repository.completeTask(task, superAdminUser, false)
                                    }
                                },
                                onApproveSuggestion = { approve ->
                                    scope.launch { repository.approveTaskSuggestion(task, approve) }
                                },
                                onEdit = {
                                    taskToEdit = task
                                    showTaskDialog = true
                                },
                                onDelete = {
                                    scope.launch { repository.deleteTask(task.taskId) }
                                }
                            )
                        }
                    }
                }

                // 3: All Store Items Management
                3 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allStoreItems, key = { it.itemId }) { item ->
                            StoreItemCard(
                                item = item,
                                currentUser = superAdminUser,
                                isAdmin = true,
                                onRequestPurchase = {},
                                onEdit = {
                                    itemToEdit = item
                                    showStoreDialog = true
                                },
                                onDelete = {
                                    scope.launch { repository.deleteStoreItem(item.itemId) }
                                }
                            )
                        }
                    }
                }

                // 4: System-wide Approvals
                4 -> {
                    ApprovalsScreen(
                        joinRequests = allJoinRequests,
                        tasks = allTasks,
                        purchases = allPurchases,
                        familyMembers = allUsers,
                        repository = repository
                    )
                }

                // 5: System-wide Notifications
                5 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allNotifications, key = { it.notificationId }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onClick = {
                                    scope.launch { repository.markNotificationRead(notif.notificationId) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit User Points Dialog
    if (userToEditPoints != null) {
        val user = userToEditPoints!!
        var newPointsStr by remember { mutableStateOf(user.pointsBalance.toString()) }

        AlertDialog(
            onDismissRequest = { userToEditPoints = null },
            title = { Text("Edit Points: ${user.displayName}") },
            text = {
                Column {
                    Text(
                        text = "Manually adjust points balance. Super Admin has authority to correct points for any user.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPointsStr,
                        onValueChange = { newPointsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(Strings.get("points_balance")) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pts = newPointsStr.toIntOrNull() ?: user.pointsBalance
                        scope.launch {
                            repository.updateUserPoints(user.userId, pts)
                            userToEditPoints = null
                        }
                    }
                ) {
                    Text(Strings.get("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEditPoints = null }) {
                    Text(Strings.get("cancel"))
                }
            }
        )
    }

    // Change Family Admin Dialog
    if (familyToChangeAdmin != null) {
        val fam = familyToChangeAdmin!!
        val candidates = allUsers.filter { it.familyId == fam.familyId || it.userId != fam.adminUserId }
        var selectedCandidateId by remember { mutableStateOf(fam.adminUserId) }
        var selectedCandidateName by remember { mutableStateOf(fam.adminName) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { familyToChangeAdmin = null },
            title = { Text("Change Admin: ${fam.name}") },
            text = {
                Column {
                    Text(
                        text = "Select a new Family Admin for this family group:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCandidateName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("New Family Admin") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            candidates.forEach { cand ->
                                DropdownMenuItem(
                                    text = { Text("${cand.displayName} (${cand.email})") },
                                    onClick = {
                                        selectedCandidateId = cand.userId
                                        selectedCandidateName = cand.displayName
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateFamilyAdmin(fam.familyId, selectedCandidateId, selectedCandidateName)
                            familyToChangeAdmin = null
                        }
                    }
                ) {
                    Text(Strings.get("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { familyToChangeAdmin = null }) {
                    Text(Strings.get("cancel"))
                }
            }
        )
    }

    if (showTaskDialog && taskToEdit != null) {
        TaskEditDialog(
            task = taskToEdit,
            currentUser = superAdminUser,
            familyMembers = allUsers,
            isAdmin = true,
            onDismiss = { showTaskDialog = false },
            onSave = { updatedTask ->
                scope.launch {
                    repository.saveTask(updatedTask, isNew = false)
                    showTaskDialog = false
                }
            }
        )
    }

    if (showStoreDialog && itemToEdit != null) {
        StoreItemDialog(
            item = itemToEdit,
            familyId = itemToEdit!!.familyId,
            currentUserId = superAdminUser.userId,
            familyMembers = allUsers,
            onDismiss = { showStoreDialog = false },
            onSave = { updatedItem ->
                scope.launch {
                    repository.saveStoreItem(updatedItem, isNew = false)
                    showStoreDialog = false
                }
            }
        )
    }
}

@Composable
fun FamilyManagementCard(
    family: Family,
    allUsers: List<User>,
    onChangeAdmin: () -> Unit,
    onDelete: () -> Unit
) {
    val memberCount = allUsers.count { it.familyId == family.familyId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = family.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Join Code: ${family.joinCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$memberCount ${Strings.get("members_count")}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Admin: ${family.adminName} (ID: ${family.adminUserId})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onChangeAdmin,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("change_admin"), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("delete"), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun UserPointsCard(
    user: User,
    onEditPoints: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    avatarTemplate = user.avatarTemplate,
                    avatarUri = user.avatarUri,
                    sizeDp = 40
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user.displayName.ifBlank { "User" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${user.email} • Role: ${user.role}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${user.pointsBalance} pts",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEditPoints) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit points")
                }
            }
        }
    }
}
