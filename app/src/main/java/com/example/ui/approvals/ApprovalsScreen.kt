package com.example.ui.approvals

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamTasksRepository
import com.example.localization.Strings
import com.example.model.JoinRequest
import com.example.model.Purchase
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.model.User
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf

@Composable
fun ApprovalsScreen(
    joinRequests: List<JoinRequest>,
    tasks: List<Task>,
    purchases: List<Purchase>,
    familyMembers: List<User>,
    repository: FamTasksRepository,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingJoinRequests = joinRequests.filter { it.status == "pending" }
    val pendingTaskSuggestions = tasks.filter { it.status == TaskStatus.PENDING_APPROVAL.value }
    val pendingTaskCompletions = tasks.filter { it.status == TaskStatus.COMPLETED_PENDING_REVIEW.value }
    val pendingPurchases = purchases.filter { it.status == "pending" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Joins (${pendingJoinRequests.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Tasks (${pendingTaskSuggestions.size + pendingTaskCompletions.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Store (${pendingPurchases.size})") }
                )
            }

            when (selectedTab) {
                0 -> {
                    if (pendingJoinRequests.isEmpty()) {
                        EmptyApprovalsPlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pendingJoinRequests, key = { it.requestId }) { req ->
                                JoinRequestApprovalCard(
                                    request = req,
                                    onApprove = {
                                        scope.launch {
                                            val res = repository.respondToJoinRequest(req.requestId, req.familyId, req.userId, true)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Join request approved")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to approve join request")
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            val res = repository.respondToJoinRequest(req.requestId, req.familyId, req.userId, false)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Join request rejected")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to reject join request")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val totalTasks = pendingTaskSuggestions + pendingTaskCompletions
                    if (totalTasks.isEmpty()) {
                        EmptyApprovalsPlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (pendingTaskSuggestions.isNotEmpty()) {
                                item {
                                    Text(
                                        text = Strings.get("pending_task_suggestions"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(pendingTaskSuggestions, key = { it.taskId }) { task ->
                                    TaskSuggestionApprovalCard(
                                        task = task,
                                        onApprove = {
                                            scope.launch {
                                                val res = repository.approveTaskSuggestion(task, true)
                                                if (res.isSuccess) {
                                                    snackbarHostState.showSnackbar("Task suggestion approved")
                                                } else {
                                                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to approve task")
                                                }
                                            }
                                        },
                                        onReject = {
                                            scope.launch {
                                                val res = repository.approveTaskSuggestion(task, false)
                                                if (res.isSuccess) {
                                                    snackbarHostState.showSnackbar("Task suggestion rejected")
                                                } else {
                                                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to reject task")
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            if (pendingTaskCompletions.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = Strings.get("pending_task_completions"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                items(pendingTaskCompletions, key = { it.taskId }) { task ->
                                    val assignee = familyMembers.find { it.userId == task.assignedToUserId }
                                    TaskCompletionApprovalCard(
                                        task = task,
                                        assignee = assignee,
                                        onApprove = {
                                            scope.launch {
                                                val res = repository.reviewTaskCompletion(task, true, assignee)
                                                if (res.isSuccess) {
                                                    snackbarHostState.showSnackbar("Task completed! Points awarded.")
                                                } else {
                                                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to award points")
                                                }
                                            }
                                        },
                                        onReject = {
                                            scope.launch {
                                                val res = repository.reviewTaskCompletion(task, false, assignee)
                                                if (res.isSuccess) {
                                                    snackbarHostState.showSnackbar("Task returned to active status")
                                                } else {
                                                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to reject completion")
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (pendingPurchases.isEmpty()) {
                        EmptyApprovalsPlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pendingPurchases, key = { it.purchaseId }) { pur ->
                                val buyer = familyMembers.find { it.userId == pur.userId }
                                PurchaseApprovalCard(
                                    purchase = pur,
                                    buyer = buyer,
                                    onApprove = {
                                        scope.launch {
                                            val res = repository.respondToPurchase(pur, true, "", buyer)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Purchase approved successfully! 🎉")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to approve purchase")
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            val res = repository.respondToPurchase(pur, false, "", buyer)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Purchase rejected. Points refunded.")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to reject purchase")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyApprovalsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.get("no_pending_approvals"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun JoinRequestApprovalCard(
    request: JoinRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
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
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = request.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = request.userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun TaskSuggestionApprovalCard(
    task: Task,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp)) {
                    Text("${task.points} pts", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Suggested by: ${task.createdByName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess), modifier = Modifier.height(36.dp)) {
                    Text(Strings.get("approve"), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onReject, modifier = Modifier.height(36.dp)) {
                    Text(Strings.get("reject"), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TaskCompletionApprovalCard(
    task: Task,
    assignee: User?,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Surface(color = EmeraldSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text("+${task.points} pts", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Completed by: ${task.assignedToUserName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess), modifier = Modifier.height(36.dp)) {
                    Text("Award Points", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onReject, modifier = Modifier.height(36.dp)) {
                    Text(Strings.get("reject"), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PurchaseApprovalCard(
    purchase: Purchase,
    buyer: User?,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    val totalCost = if (purchase.totalPoints > 0) purchase.totalPoints else purchase.pointPrice
    val qty = if (purchase.quantity > 0) purchase.quantity else 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = purchase.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp)) {
                    Text("-$totalCost pts", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Qty: $qty (${purchase.pointPrice} pts each) • Requested by: ${purchase.userName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Buyer Available Balance: ${buyer?.pointsBalance ?: 0} pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Button(
                        onClick = {
                            isProcessing = true
                            onApprove()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(Strings.get("approve"), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            isProcessing = true
                            onReject()
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(Strings.get("reject"), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
