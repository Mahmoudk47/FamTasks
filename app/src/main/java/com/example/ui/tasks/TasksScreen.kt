package com.example.ui.tasks

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamTasksRepository
import com.example.localization.Strings
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    currentUser: User,
    tasks: List<Task>,
    familyMembers: List<User>,
    repository: FamTasksRepository,
    isSuperAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isAdmin = isSuperAdmin || currentUser.role == UserRole.FAMILY_ADMIN.value
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Tasks, 1: All Tasks, 2: Suggestions
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()

    val filteredTasks = when (selectedTab) {
        0 -> tasks.filter { it.assignedToUserId == currentUser.userId && it.status != TaskStatus.PENDING_APPROVAL.value }
        1 -> tasks.filter { it.status != TaskStatus.PENDING_APPROVAL.value }
        else -> tasks.filter { it.status == TaskStatus.PENDING_APPROVAL.value }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showTaskDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isAdmin) Strings.get("add_task") else Strings.get("suggest_task")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.get("my_tasks")) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(Strings.get("all_tasks")) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        val pendingCount = tasks.count { it.status == TaskStatus.PENDING_APPROVAL.value }
                        Text("${Strings.get("suggested_tasks")}${if (pendingCount > 0) " ($pendingCount)" else ""}")
                    }
                )
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Strings.get("no_tasks_found"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks, key = { it.taskId }) { task ->
                        TaskCard(
                            task = task,
                            currentUser = currentUser,
                            isAdmin = isAdmin,
                            onComplete = {
                                scope.launch {
                                    repository.completeTask(
                                        task = task,
                                        currentUser = currentUser,
                                        requiresAdminReview = !isAdmin
                                    )
                                }
                            },
                            onApproveSuggestion = { approve ->
                                scope.launch {
                                    repository.approveTaskSuggestion(task, approve)
                                }
                            },
                            onEdit = {
                                taskToEdit = task
                                showTaskDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    repository.deleteTask(task.taskId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        TaskEditDialog(
            task = taskToEdit,
            currentUser = currentUser,
            familyMembers = familyMembers,
            isAdmin = isAdmin,
            onDismiss = { showTaskDialog = false },
            onSave = { savedTask ->
                scope.launch {
                    repository.saveTask(savedTask, isNew = taskToEdit == null)
                    showTaskDialog = false
                }
            }
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    currentUser: User,
    isAdmin: Boolean,
    onComplete: () -> Unit,
    onApproveSuggestion: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAssignedToMe = task.assignedToUserId == currentUser.userId
    val isPendingApproval = task.status == TaskStatus.PENDING_APPROVAL.value
    val isPendingReview = task.status == TaskStatus.COMPLETED_PENDING_REVIEW.value
    val isCompleted = task.status == TaskStatus.COMPLETED.value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.taskId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title + Points Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Points Chip
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.points} pts",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Assignee & Scheduling info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.assignedToUserId == currentUser.userId) "${task.assignedToUserName} (${Strings.get("assign_to_me")})"
                        else task.assignedToUserName.ifBlank { "Unassigned" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (task.scheduleType.isNotBlank() && task.scheduleType != "one_time") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.scheduleType.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (task.isRecurring) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.recurringInterval.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (task.status) {
                        TaskStatus.COMPLETED.value -> EmeraldSuccess.copy(alpha = 0.15f)
                        TaskStatus.COMPLETED_PENDING_REVIEW.value -> Color(0xFFFEF3C7)
                        TaskStatus.PENDING_APPROVAL.value -> Color(0xFFE0E7FF)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (task.status) {
                                TaskStatus.COMPLETED.value -> Icons.Default.CheckCircle
                                TaskStatus.COMPLETED_PENDING_REVIEW.value -> Icons.Default.HourglassBottom
                                else -> Icons.Default.AssignmentTurnedIn
                            },
                            contentDescription = null,
                            tint = when (task.status) {
                                TaskStatus.COMPLETED.value -> EmeraldSuccess
                                TaskStatus.COMPLETED_PENDING_REVIEW.value -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (task.status) {
                                TaskStatus.COMPLETED.value -> Strings.get("completed")
                                TaskStatus.COMPLETED_PENDING_REVIEW.value -> Strings.get("pending_review")
                                TaskStatus.PENDING_APPROVAL.value -> Strings.get("pending_approval")
                                else -> Strings.get("active")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (task.status) {
                                TaskStatus.COMPLETED.value -> EmeraldSuccess
                                TaskStatus.COMPLETED_PENDING_REVIEW.value -> Color(0xFF92400E)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPendingApproval && isAdmin) {
                        Button(
                            onClick = { onApproveSuggestion(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(Strings.get("approve"), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onApproveSuggestion(false) },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(Strings.get("reject"), fontSize = 12.sp)
                        }
                    } else if (!isCompleted && !isPendingReview && (isAssignedToMe || isAdmin)) {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("mark_completed_${task.taskId}")
                        ) {
                            Text(Strings.get("mark_completed"), fontSize = 12.sp)
                        }
                    }

                    if (isAdmin) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    task: Task?,
    currentUser: User,
    familyMembers: List<User>,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var pointsStr by remember { mutableStateOf((task?.points ?: 10).toString()) }
    var assignedUserId by remember { mutableStateOf(task?.assignedToUserId ?: currentUser.userId) }
    var assignedUserName by remember { mutableStateOf(task?.assignedToUserName ?: currentUser.displayName) }
    var scheduleType by remember { mutableStateOf(task?.scheduleType ?: "one_time") }
    var scheduleDetails by remember { mutableStateOf(task?.scheduleDetails ?: "") }
    var isRecurring by remember { mutableStateOf(task?.isRecurring ?: false) }
    var recurringInterval by remember { mutableStateOf(task?.recurringInterval ?: "daily") }

    var memberDropdownExpanded by remember { mutableStateOf(false) }
    var scheduleDropdownExpanded by remember { mutableStateOf(false) }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (task == null) {
                    if (isAdmin) Strings.get("add_task") else Strings.get("suggest_task")
                } else "Edit Task",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Strings.get("task_name")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.get("task_description")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = pointsStr,
                    onValueChange = { pointsStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(Strings.get("points")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_points_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (isAdmin) {
                    // Assignee Selector
                    ExposedDropdownMenuBox(
                        expanded = memberDropdownExpanded,
                        onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = assignedUserName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Strings.get("assign_to")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false }
                        ) {
                            // Assign to self
                            DropdownMenuItem(
                                text = { Text("${currentUser.displayName} (${Strings.get("assign_to_me")})") },
                                onClick = {
                                    assignedUserId = currentUser.userId
                                    assignedUserName = currentUser.displayName
                                    memberDropdownExpanded = false
                                }
                            )
                            familyMembers.filter { it.userId != currentUser.userId }.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.displayName) },
                                    onClick = {
                                        assignedUserId = member.userId
                                        assignedUserName = member.displayName
                                        memberDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Schedule Type Selector
                    ExposedDropdownMenuBox(
                        expanded = scheduleDropdownExpanded,
                        onExpandedChange = { scheduleDropdownExpanded = !scheduleDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = scheduleType.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Strings.get("schedule_type")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = scheduleDropdownExpanded,
                            onDismissRequest = { scheduleDropdownExpanded = false }
                        ) {
                            listOf("one_time", "scheduled", "daily", "weekly", "custom").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        scheduleType = type
                                        scheduleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (scheduleType != "one_time") {
                        OutlinedTextField(
                            value = scheduleDetails,
                            onValueChange = { scheduleDetails = it },
                            label = { Text(Strings.get("schedule_details")) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Recurring task toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Strings.get("recurring"), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    }

                    if (isRecurring) {
                        ExposedDropdownMenuBox(
                            expanded = intervalDropdownExpanded,
                            onExpandedChange = { intervalDropdownExpanded = !intervalDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = recurringInterval.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Repeat Frequency") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = intervalDropdownExpanded,
                                onDismissRequest = { intervalDropdownExpanded = false }
                            ) {
                                listOf("daily", "weekly", "monthly").forEach { intv ->
                                    DropdownMenuItem(
                                        text = { Text(intv.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            recurringInterval = intv
                                            intervalDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val pts = pointsStr.toIntOrNull() ?: 10
                    val initialStatus = if (task != null) task.status else {
                        if (isAdmin) TaskStatus.ACTIVE.value else TaskStatus.PENDING_APPROVAL.value
                    }

                    val updatedTask = Task(
                        taskId = task?.taskId ?: "",
                        familyId = currentUser.familyId,
                        title = title.trim(),
                        description = description.trim(),
                        points = pts,
                        assignedToUserId = if (isAdmin) assignedUserId else currentUser.userId,
                        assignedToUserName = if (isAdmin) assignedUserName else currentUser.displayName,
                        createdByUserId = task?.createdByUserId ?: currentUser.userId,
                        createdByName = task?.createdByName ?: currentUser.displayName,
                        status = initialStatus,
                        scheduleType = scheduleType,
                        scheduleDetails = scheduleDetails.trim(),
                        isRecurring = isRecurring,
                        recurringInterval = if (isRecurring) recurringInterval else "none"
                    )
                    onSave(updatedTask)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text(Strings.get("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("cancel"))
            }
        }
    )
}
