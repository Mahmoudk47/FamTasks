package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.FamTasksRepository
import com.example.localization.AppLanguage
import com.example.localization.LocalAppLanguage
import com.example.localization.Strings
import com.example.model.AppNotification
import com.example.model.Family
import com.example.model.JoinRequest
import com.example.model.Purchase
import com.example.model.StoreItem
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.approvals.ApprovalsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.FamilyOnboardingScreen
import com.example.ui.components.FamTasksLogo
import com.example.ui.components.LeaderboardView
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.store.StoreScreen
import com.example.ui.superadmin.SuperAdminDashboard
import com.example.ui.tasks.TasksScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun MainApp(
    repository: FamTasksRepository
) {
    var appLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
    var themeMode by remember { mutableStateOf("system") }
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    var isSuperAdminActive by remember { mutableStateOf(Firebase.auth.currentUser?.email == "admin@admin.com") }
    var currentFirebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var activeAppUser by remember { mutableStateOf<User?>(null) }

    DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            currentFirebaseUser = auth.currentUser
            if (auth.currentUser == null) {
                activeAppUser = null
                isSuperAdminActive = false
            } else if (auth.currentUser?.email == "admin@admin.com") {
                isSuperAdminActive = true
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
        CompositionLocalProvider(
            LocalAppLanguage provides appLanguage,
            LocalLayoutDirection provides appLanguage.layoutDirection
        ) {
            if (isSuperAdminActive) {
                // Super Admin Full Screen Dashboard
                val families by repository.observeAllFamilies().collectAsState(initial = emptyList())
                val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())
                val allTasks by repository.observeAllTasks().collectAsState(initial = emptyList())
                val allStoreItems by repository.observeAllStoreItems().collectAsState(initial = emptyList())
                val allPurchases by repository.observeAllPurchases().collectAsState(initial = emptyList())
                val allNotifications by repository.observeAllNotifications().collectAsState(initial = emptyList())

                SuperAdminDashboard(
                    families = families,
                    allUsers = allUsers,
                    allTasks = allTasks,
                    allStoreItems = allStoreItems,
                    allPurchases = allPurchases,
                    allJoinRequests = emptyList(),
                    allNotifications = allNotifications,
                    repository = repository,
                    onExitDashboard = { isSuperAdminActive = false }
                )
            } else if (activeAppUser == null && currentFirebaseUser == null) {
                // Unauthenticated session
                AuthScreen(
                    repository = repository,
                    onSuperAdminLogin = { isSuperAdminActive = true },
                    onAuthSuccess = { user ->
                        activeAppUser = user
                        currentFirebaseUser = Firebase.auth.currentUser
                    }
                )
            } else {
                // Authenticated normal user session
                val resolvedUserId = activeAppUser?.userId ?: currentFirebaseUser!!.uid
                AuthenticatedAppFlow(
                    userId = resolvedUserId,
                    initialUser = activeAppUser,
                    repository = repository,
                    currentLanguage = appLanguage,
                    currentThemeMode = themeMode,
                    onLanguageChange = { appLanguage = it },
                    onThemeModeChange = { themeMode = it },
                    onSignOut = {
                        activeAppUser = null
                        currentFirebaseUser = null
                        Firebase.auth.signOut()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedAppFlow(
    userId: String,
    initialUser: User? = null,
    repository: FamTasksRepository,
    currentLanguage: AppLanguage,
    currentThemeMode: String,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val userProfile by repository.observeUser(userId).collectAsState(initial = initialUser)

    if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = userProfile!!
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (user.familyId.isBlank()) {
        FamilyOnboardingScreen(
            currentUser = user,
            repository = repository,
            onFamilyJoined = { fam ->
                scope.launch {
                    repository.saveUser(user.copy(familyId = fam.familyId, role = fam.adminUserId.let { if (it == user.userId) UserRole.FAMILY_ADMIN.value else UserRole.FAMILY_MEMBER.value }))
                }
            }
        )
        return
    }

    // User is in a family
    val family by repository.observeFamily(user.familyId).collectAsState(initial = null)
    val familyMembers by repository.observeFamilyMembers(user.familyId).collectAsState(initial = emptyList())
    val tasks by repository.observeTasks(user.familyId).collectAsState(initial = emptyList())
    val storeItems by repository.observeStoreItems(user.familyId).collectAsState(initial = emptyList())
    val purchases by repository.observePurchases(user.familyId).collectAsState(initial = emptyList())
    val joinRequests by repository.observeJoinRequests(user.familyId).collectAsState(initial = emptyList())
    val notifications by repository.observeNotifications(userId, user.familyId).collectAsState(initial = emptyList())

    val isAdmin = user.role == UserRole.FAMILY_ADMIN.value

    var currentScreenIndex by remember { mutableIntStateOf(0) }

    // Calculate badge counts
    val pendingApprovalsCount = if (isAdmin) {
        joinRequests.count { it.status == "pending" } +
        tasks.count { it.status == TaskStatus.PENDING_APPROVAL.value || it.status == TaskStatus.COMPLETED_PENDING_REVIEW.value } +
        purchases.count { it.status == "pending" }
    } else 0

    val unreadNotificationsCount = notifications.count { !it.isRead }

    BackHandler(enabled = currentScreenIndex != 0) {
        currentScreenIndex = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FamTasksLogo(sizeDp = 26)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (currentScreenIndex) {
                                0 -> Strings.get("tasks")
                                1 -> Strings.get("leaderboard")
                                2 -> Strings.get("store")
                                3 -> Strings.get("approvals")
                                4 -> Strings.get("notifications")
                                else -> Strings.get("settings")
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentScreenIndex == 0,
                    onClick = { currentScreenIndex = 0 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = Strings.get("tasks")) },
                    label = null,
                    alwaysShowLabel = false,
                    modifier = Modifier.testTag("nav_tasks")
                )
                NavigationBarItem(
                    selected = currentScreenIndex == 1,
                    onClick = { currentScreenIndex = 1 },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = Strings.get("leaderboard")) },
                    label = null,
                    alwaysShowLabel = false,
                    modifier = Modifier.testTag("nav_leaderboard")
                )
                NavigationBarItem(
                    selected = currentScreenIndex == 2,
                    onClick = { currentScreenIndex = 2 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = Strings.get("store")) },
                    label = null,
                    alwaysShowLabel = false,
                    modifier = Modifier.testTag("nav_store")
                )
                if (isAdmin) {
                    NavigationBarItem(
                        selected = currentScreenIndex == 3,
                        onClick = { currentScreenIndex = 3 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingApprovalsCount > 0) {
                                        Badge { Text("$pendingApprovalsCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = Strings.get("approvals"))
                            }
                        },
                        label = null,
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_approvals")
                    )
                }
                NavigationBarItem(
                    selected = currentScreenIndex == 4,
                    onClick = { currentScreenIndex = 4 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text("$unreadNotificationsCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = Strings.get("notifications"))
                        }
                    },
                    label = null,
                    alwaysShowLabel = false,
                    modifier = Modifier.testTag("nav_notifications")
                )
                NavigationBarItem(
                    selected = currentScreenIndex == 5,
                    onClick = { currentScreenIndex = 5 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = Strings.get("settings")) },
                    label = null,
                    alwaysShowLabel = false,
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreenIndex) {
                0 -> TasksScreen(
                    currentUser = user,
                    tasks = tasks,
                    familyMembers = familyMembers,
                    repository = repository
                )
                1 -> LeaderboardView(
                    members = familyMembers,
                    currentUserId = user.userId,
                    modifier = Modifier.padding(16.dp)
                )
                2 -> StoreScreen(
                    currentUser = user,
                    items = storeItems,
                    purchases = purchases,
                    familyMembers = familyMembers,
                    repository = repository
                )
                3 -> {
                    if (isAdmin) {
                        ApprovalsScreen(
                            joinRequests = joinRequests,
                            tasks = tasks,
                            purchases = purchases,
                            familyMembers = familyMembers,
                            repository = repository
                        )
                    } else {
                        currentScreenIndex = 0
                    }
                }
                4 -> NotificationsScreen(
                    notifications = notifications,
                    repository = repository
                )
                5 -> SettingsScreen(
                    currentUser = user,
                    currentFamily = family,
                    currentLanguage = currentLanguage,
                    currentThemeMode = currentThemeMode,
                    onLanguageChange = onLanguageChange,
                    onThemeModeChange = onThemeModeChange,
                    onSignOut = {
                        scope.launch {
                            val credentialManager = CredentialManager.create(context)
                            try {
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            } catch (e: Exception) {
                                // ignore
                            }
                            onSignOut()
                        }
                    },
                    repository = repository
                )
            }
        }
    }
}
