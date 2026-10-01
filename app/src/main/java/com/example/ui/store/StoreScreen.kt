package com.example.ui.store

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FamTasksRepository
import com.example.localization.Strings
import com.example.model.Purchase
import com.example.model.StoreItem
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    currentUser: User,
    items: List<StoreItem>,
    purchases: List<Purchase>,
    familyMembers: List<User>,
    repository: FamTasksRepository,
    isSuperAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isAdmin = isSuperAdmin || currentUser.role == UserRole.FAMILY_ADMIN.value
    var selectedItemForDetails by remember { mutableStateOf<StoreItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<StoreItem?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val requestCancelledMsg = Strings.get("request_cancelled")

    // Keep selectedItemForDetails synchronized with live database state (e.g. remaining inventory updates)
    val activeDetailsItem = selectedItemForDetails?.let { sel ->
        items.find { it.itemId == sel.itemId } ?: sel
    }

    val visibleItems = if (isAdmin) {
        items
    } else {
        items.filter { it.targetType == "whole_family" || it.targetUserId == currentUser.userId }
    }

    val userPurchases = purchases.filter { it.userId == currentUser.userId }
        .sortedByDescending { it.createdAt?.seconds ?: 0L }

    // If viewing dedicated details page:
    if (activeDetailsItem != null) {
        BackHandler {
            selectedItemForDetails = null
        }
        StoreItemDetailsScreen(
            item = activeDetailsItem,
            currentUser = currentUser,
            repository = repository,
            onBack = { selectedItemForDetails = null },
            onPurchaseSuccess = { msg ->
                selectedItemForDetails = null
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isAdmin && selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        itemToEdit = null
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_store_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = Strings.get("add_reward"))
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Points Balance Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.get("points_balance"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentUser.pointsBalance}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Strings.get("earned"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "+${currentUser.totalPointsEarned}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Strings.get("spent"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "-${currentUser.totalPointsSpent}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Tab row: Rewards Store vs My Requests
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.get("store")) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val pendingCount = userPurchases.count { it.status == "pending" }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Strings.get("my_orders"))
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge { Text("$pendingCount") }
                            }
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // STORE ITEMS GRID
                if (visibleItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Strings.get("no_items_found"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleItems, key = { it.itemId }) { item ->
                            StoreItemCard(
                                item = item,
                                currentUser = currentUser,
                                isAdmin = isAdmin,
                                onClick = {
                                    selectedItemForDetails = item
                                },
                                onEdit = {
                                    itemToEdit = item
                                    showAddDialog = true
                                },
                                onDelete = {
                                    scope.launch {
                                        repository.deleteStoreItem(item.itemId)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // MY REQUESTS LIST
                if (userPurchases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No purchase requests yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(userPurchases, key = { it.purchaseId }) { purchase ->
                            UserPurchaseItemCard(
                                purchase = purchase,
                                onCancel = {
                                    scope.launch {
                                        val cancelRes = repository.cancelPurchase(purchase.purchaseId, currentUser.userId)
                                        if (cancelRes.isSuccess) {
                                            snackbarHostState.showSnackbar(requestCancelledMsg)
                                        } else {
                                            snackbarHostState.showSnackbar(cancelRes.exceptionOrNull()?.message ?: "Failed to cancel")
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

    if (showAddDialog) {
        StoreItemDialog(
            item = itemToEdit,
            familyId = currentUser.familyId,
            currentUserId = currentUser.userId,
            familyMembers = familyMembers,
            onDismiss = { showAddDialog = false },
            onSave = { savedItem ->
                scope.launch {
                    repository.saveStoreItem(savedItem, isNew = itemToEdit == null)
                    showAddDialog = false
                }
            }
        )
    }
}

/**
 * Modern E-Commerce Style Store Item Card
 * - Product image fills thumbnail area with ContentScale.Crop and subtle fade into card background.
 * - Displays name, price, available inventory ("X left" / "Out of stock"), audience.
 * - Entire card clicks to open dedicated Details page.
 */
@Composable
fun StoreItemCard(
    item: StoreItem,
    currentUser: User,
    isAdmin: Boolean,
    onClick: () -> Unit = {},
    onRequestPurchase: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isOutOfStock = item.quantity <= 0
    val itemsLeftKey = if (item.quantity == 1) "item_left" else "items_left"
    val availabilityText = if (isOutOfStock) {
        Strings.get("out_of_stock")
    } else {
        "${item.quantity} ${Strings.get(itemsLeftKey)}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("store_item_${item.itemId}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Modern E-Commerce Product Thumbnail with Fade Effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                val primaryImg = item.primaryImage
                if (primaryImg.isNotBlank()) {
                    AsyncImage(
                        model = primaryImg,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Subtle bottom fade scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Stock availability badge top-end
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOutOfStock) MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    else Color(0xFF1E293B).copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = availabilityText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Target badge top-start
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.targetType == "whole_family") Icons.Default.Groups else Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.targetType == "whole_family") Strings.get("whole_family")
                            else item.targetUserName.ifBlank { Strings.get("specific_person") },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Card Body
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.pointPrice} ${Strings.get("points")}",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    if (isAdmin) {
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Store Item Details Page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreItemDetailsScreen(
    item: StoreItem,
    currentUser: User,
    repository: FamTasksRepository,
    onBack: () -> Unit,
    onPurchaseSuccess: (String) -> Unit
) {
    val allImages = if (item.images.isNotEmpty()) item.images else if (item.imageUrl.isNotBlank()) listOf(item.imageUrl) else emptyList()
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var quantity by remember { mutableIntStateOf(1) }
    var isSubmitting by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val availableQty = item.quantity
    val isOutOfStock = availableQty <= 0
    val totalCost = item.pointPrice * quantity
    val hasEnoughPoints = currentUser.pointsBalance >= totalCost
    val canPurchase = !isOutOfStock && hasEnoughPoints && quantity in 1..availableQty && !isSubmitting

    val itemsLeftKey = if (availableQty == 1) "item_left" else "items_left"
    val availabilityText = if (isOutOfStock) {
        Strings.get("out_of_stock")
    } else {
        "$availableQty ${Strings.get(itemsLeftKey)}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Product Image with subtle fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                val currentImg = allImages.getOrNull(selectedImageIndex) ?: item.primaryImage
                if (currentImg.isNotBlank()) {
                    AsyncImage(
                        model = currentImg,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                // Subtle bottom gradient fade
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Stock Badge Overlay
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOutOfStock) MaterialTheme.colorScheme.error else Color(0xFF0F172A).copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text(
                        text = availabilityText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Multiple Images Gallery Strip
            if (allImages.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allImages.indices.toList()) { index ->
                        val isSelected = index == selectedImageIndex
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedImageIndex = index }
                        ) {
                            AsyncImage(
                                model = allImages[index],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Details Body
            Column(modifier = Modifier.padding(20.dp)) {
                // Item Name & Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.pointPrice} ${Strings.get("points")}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Audience tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.targetType == "whole_family") Icons.Default.Groups else Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.targetType == "whole_family") Strings.get("whole_family")
                            else item.targetUserName.ifBlank { Strings.get("specific_person") },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Strings.get("task_description"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quantity Selector Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Strings.get("quantity"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // [-] Qty [+] Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    enabled = quantity > 1 && !isSubmitting,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                                }

                                Text(
                                    text = "$quantity",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                OutlinedButton(
                                    onClick = { if (quantity < availableQty) quantity++ },
                                    enabled = quantity < availableQty && !isSubmitting,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Total Points Calculation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Strings.get("total_points"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalCost ${Strings.get("points")}",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Strings.get("available_points"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${currentUser.pointsBalance} ${Strings.get("points")}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasEnoughPoints) EmeraldSuccess else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Note explaining immediate reservation
                Text(
                    text = "• ${Strings.get("points_reserved_note")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (actionError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = actionError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!hasEnoughPoints) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.get("not_enough_points"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Request Purchase Button (IMMEDIATELY DISABLED on click to prevent multiple clicks)
                Button(
                    onClick = {
                        if (!canPurchase || isSubmitting) return@Button
                        isSubmitting = true
                        actionError = null
                        scope.launch {
                            val result = repository.requestPurchase(
                                item = item,
                                user = currentUser,
                                requestedQuantity = quantity
                            )
                            isSubmitting = false
                            if (result.isSuccess) {
                                onPurchaseSuccess("Purchase requested! Points reserved.")
                            } else {
                                actionError = result.exceptionOrNull()?.message ?: "Failed to submit purchase request"
                            }
                        }
                    },
                    enabled = canPurchase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("request_purchase_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOutOfStock) Strings.get("out_of_stock") else Strings.get("request_purchase"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * User purchase card showing status and "Cancel Request" action for pending purchases
 */
@Composable
fun UserPurchaseItemCard(
    purchase: Purchase,
    onCancel: () -> Unit
) {
    var isCancelling by remember { mutableStateOf(false) }
    val totalCost = if (purchase.totalPoints > 0) purchase.totalPoints else purchase.pointPrice
    val qty = if (purchase.quantity > 0) purchase.quantity else 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = purchase.itemName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Status Badge
                val (statusColor, statusBg, statusText) = when (purchase.status) {
                    "approved" -> Triple(EmeraldSuccess, EmeraldSuccess.copy(alpha = 0.15f), Strings.get("purchased"))
                    "rejected" -> Triple(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), Strings.get("rejected_purchase"))
                    "cancelled" -> Triple(Color(0xFF64748B), Color(0xFFF1F5F9), "Cancelled")
                    else -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), Strings.get("purchase_pending"))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Qty: $qty • $totalCost ${Strings.get("points")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Cancel Request action only for pending requests
                if (purchase.status == "pending") {
                    OutlinedButton(
                        onClick = {
                            isCancelling = true
                            onCancel()
                        },
                        enabled = !isCancelling,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                        } else {
                            Text(Strings.get("cancel_request"), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Admin Dialog to Add or Edit a Store Item with Quantity and Multiple Images
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreItemDialog(
    item: StoreItem?,
    familyId: String,
    currentUserId: String,
    familyMembers: List<User>,
    onDismiss: () -> Unit,
    onSave: (StoreItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var priceStr by remember { mutableStateOf((item?.pointPrice ?: 50).toString()) }
    var quantityStr by remember { mutableStateOf((item?.quantity ?: 10).toString()) }
    var targetType by remember { mutableStateOf(item?.targetType ?: "whole_family") }
    var targetUserId by remember { mutableStateOf(item?.targetUserId ?: "") }
    var targetUserName by remember { mutableStateOf(item?.targetUserName ?: "") }

    val imagesList = remember {
        mutableStateListOf<String>().apply {
            if (item != null) {
                if (item.images.isNotEmpty()) addAll(item.images)
                else if (item.imageUrl.isNotBlank()) add(item.imageUrl)
            }
        }
    }

    var newImageUrlInput by remember { mutableStateOf("") }
    var showAddImageField by remember { mutableStateOf(false) }

    var audienceDropdownExpanded by remember { mutableStateOf(false) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (item == null) Strings.get("add_reward") else "Edit Store Item",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.get("item_name")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.get("task_description")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(Strings.get("item_price")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_price_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(Strings.get("quantity")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_quantity_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Audience: Whole Family vs Specific Person
                ExposedDropdownMenuBox(
                    expanded = audienceDropdownExpanded,
                    onExpandedChange = { audienceDropdownExpanded = !audienceDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (targetType == "whole_family") Strings.get("whole_family") else Strings.get("specific_person"),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.get("target_audience")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = audienceDropdownExpanded,
                        onDismissRequest = { audienceDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.get("whole_family")) },
                            onClick = {
                                targetType = "whole_family"
                                targetUserId = ""
                                targetUserName = ""
                                audienceDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.get("specific_person")) },
                            onClick = {
                                targetType = "specific_person"
                                audienceDropdownExpanded = false
                            }
                        )
                    }
                }

                if (targetType == "specific_person") {
                    ExposedDropdownMenuBox(
                        expanded = memberDropdownExpanded,
                        onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = targetUserName.ifBlank { "Select Family Member" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assignee") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false }
                        ) {
                            familyMembers.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.displayName) },
                                    onClick = {
                                        targetUserId = member.userId
                                        targetUserName = member.displayName
                                        memberDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Images Section
                Text(
                    text = "${Strings.get("images")} (${imagesList.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (imagesList.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(imagesList.indices.toList()) { idx ->
                            val imgUrl = imagesList[idx]
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (idx == 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = Strings.get("primary_image"),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { imagesList.removeAt(idx) },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Add Image Option
                if (showAddImageField) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newImageUrlInput,
                            onValueChange = { newImageUrlInput = it },
                            label = { Text("Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (newImageUrlInput.isNotBlank()) {
                                        imagesList.add(newImageUrlInput.trim())
                                        newImageUrlInput = ""
                                        showAddImageField = false
                                    }
                                },
                                enabled = newImageUrlInput.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add")
                            }
                            TextButton(onClick = { showAddImageField = false }) {
                                Text(Strings.get("cancel"))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddImageField = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.get("add_image"))
                    }
                }

                // Quick presets helper for convenience
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Chocolate" to "https://images.unsplash.com/photo-1549007994-cb92caebd54b?w=600",
                        "Movie Night" to "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=600",
                        "Game" to "https://images.unsplash.com/photo-1612287232230-68d7120302fb?w=600"
                    )
                    presets.forEach { (label, url) ->
                        OutlinedButton(
                            onClick = {
                                if (!imagesList.contains(url)) {
                                    imagesList.add(url)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(label, fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val price = priceStr.toIntOrNull() ?: 50
                    val qty = (quantityStr.toIntOrNull() ?: 10).coerceAtLeast(0)
                    val updatedItem = StoreItem(
                        itemId = item?.itemId ?: "",
                        familyId = familyId,
                        name = name.trim(),
                        description = description.trim(),
                        pointPrice = price,
                        quantity = qty,
                        images = imagesList.toList(),
                        imageUrl = imagesList.firstOrNull() ?: item?.imageUrl ?: "",
                        targetType = targetType,
                        targetUserId = if (targetType == "specific_person") targetUserId else "",
                        targetUserName = if (targetType == "specific_person") targetUserName else "",
                        createdByUserId = item?.createdByUserId ?: currentUserId
                    )
                    onSave(updatedItem)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_store_item_button")
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
