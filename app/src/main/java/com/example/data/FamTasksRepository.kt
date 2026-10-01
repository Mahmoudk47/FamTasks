package com.example.data

import com.example.model.AppNotification
import com.example.model.Family
import com.example.model.JoinRequest
import com.example.model.Purchase
import com.example.model.StoreItem
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.model.User
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FamTasksRepository(
    private val db: FirebaseFirestore
) {
    private val auth = Firebase.auth
    private val localUserCache = java.util.concurrent.ConcurrentHashMap<String, User>()

    fun getCurrentFirebaseUid(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: "super_admin_fixed_id"
    }

    // --- Users ---
    fun observeUser(userId: String): Flow<User?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(User::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: localUserCache[userId] }
            .catch { error ->
                val cached = localUserCache[userId]
                if (cached != null) {
                    emit(cached)
                } else {
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, "users/$userId")
                    emit(null)
                }
            }

    fun observeAllUsers(): Flow<List<User>> =
        db.collection("users")
            .snapshots()
            .map { it.toObjects(User::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "users")
                throw error
            }

    fun observeFamilyMembers(familyId: String): Flow<List<User>> =
        db.collection("users")
            .whereEqualTo("familyId", familyId)
            .snapshots()
            .map { it.toObjects(User::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "users?familyId=$familyId")
                throw error
            }

    suspend fun saveUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        localUserCache[user.email.trim().lowercase()] = user
        localUserCache[user.userId] = user
        try {
            val docRef = db.collection("users").document(user.userId)
            val data = mapOf(
                "userId" to user.userId,
                "email" to user.email,
                "firstName" to user.firstName,
                "secondName" to user.secondName,
                "displayName" to user.displayName.ifBlank { "${user.firstName} ${user.secondName}".trim() },
                "password" to user.password,
                "avatarTemplate" to user.avatarTemplate,
                "avatarUri" to user.avatarUri,
                "role" to user.role,
                "familyId" to user.familyId,
                "pointsBalance" to user.pointsBalance,
                "totalPointsEarned" to user.totalPointsEarned,
                "totalPointsSpent" to user.totalPointsSpent,
                "notifyNewTasks" to user.notifyNewTasks,
                "notifyTaskChanges" to user.notifyTaskChanges,
                "notifyStoreItems" to user.notifyStoreItems,
                "notifyPurchases" to user.notifyPurchases,
                "notifyApprovals" to user.notifyApprovals,
                "preferredLanguage" to user.preferredLanguage,
                "themeMode" to user.themeMode,
                "createdAt" to (user.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${user.userId}")
            // If offline, still allow user creation to succeed locally
            Result.success(Unit)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        firstName: String,
        secondName: String,
        avatarTemplate: String,
        avatarUri: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedFirst = firstName.trim()
        val trimmedSecond = secondName.trim()
        val newDisplayName = "$trimmedFirst $trimmedSecond".trim()

        try {
            val docRef = db.collection("users").document(userId)
            val updates = mapOf(
                "firstName" to trimmedFirst,
                "secondName" to trimmedSecond,
                "displayName" to newDisplayName.ifBlank { "User" },
                "avatarTemplate" to avatarTemplate,
                "avatarUri" to avatarUri,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()

            val cached = localUserCache[userId]
            if (cached != null) {
                val updated = cached.copy(
                    firstName = trimmedFirst,
                    secondName = trimmedSecond,
                    displayName = newDisplayName.ifBlank { cached.displayName },
                    avatarTemplate = avatarTemplate,
                    avatarUri = avatarUri
                )
                localUserCache[userId] = updated
                localUserCache[updated.email.trim().lowercase()] = updated
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User?> = withContext(Dispatchers.IO) {
        val cached = localUserCache[userId]
        try {
            val doc = db.collection("users").document(userId).get().await()
            val user = doc.toObject(User::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (user != null) {
                localUserCache[userId] = user
                localUserCache[user.email.trim().lowercase()] = user
                Result.success(user)
            } else if (cached != null) {
                Result.success(cached)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId")
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun findUserByEmail(email: String): Result<User?> = withContext(Dispatchers.IO) {
        val normalized = email.trim().lowercase()
        val cached = localUserCache[normalized]
        try {
            val query = db.collection("users")
                .whereEqualTo("email", normalized)
                .limit(1)
                .get()
                .await()
            val user = query.documents.firstOrNull()?.toObject(User::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (user != null) {
                localUserCache[normalized] = user
                localUserCache[user.userId] = user
                Result.success(user)
            } else if (cached != null) {
                Result.success(cached)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users?email=$email")
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateUserPoints(userId: String, newBalance: Int, totalEarnedDelta: Int = 0, totalSpentDelta: Int = 0): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("users").document(userId)
            val updates = mutableMapOf<String, Any>(
                "pointsBalance" to newBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (totalEarnedDelta > 0) {
                updates["totalPointsEarned"] = FieldValue.increment(totalEarnedDelta.toLong())
            }
            if (totalSpentDelta > 0) {
                updates["totalPointsSpent"] = FieldValue.increment(totalSpentDelta.toLong())
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        userId: String,
        language: String? = null,
        theme: String? = null,
        notifyNewTasks: Boolean? = null,
        notifyTaskChanges: Boolean? = null,
        notifyStoreItems: Boolean? = null,
        notifyPurchases: Boolean? = null,
        notifyApprovals: Boolean? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            language?.let { updates["preferredLanguage"] = it }
            theme?.let { updates["themeMode"] = it }
            notifyNewTasks?.let { updates["notifyNewTasks"] = it }
            notifyTaskChanges?.let { updates["notifyTaskChanges"] = it }
            notifyStoreItems?.let { updates["notifyStoreItems"] = it }
            notifyPurchases?.let { updates["notifyPurchases"] = it }
            notifyApprovals?.let { updates["notifyApprovals"] = it }

            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    // --- Families ---
    fun observeFamily(familyId: String): Flow<Family?> =
        db.collection("families").document(familyId)
            .snapshots()
            .map { it.toObject(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, "families/$familyId")
                throw error
            }

    fun observeAllFamilies(): Flow<List<Family>> =
        db.collection("families")
            .snapshots()
            .map { it.toObjects(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "families")
                throw error
            }

    suspend fun createFamily(name: String, adminUser: User): Result<Family> = withContext(Dispatchers.IO) {
        try {
            val familyId = "fam_" + UUID.randomUUID().toString().replace("-", "").take(8)
            val joinCode = generateJoinCode()
            val family = Family(
                familyId = familyId,
                name = name.trim(),
                joinCode = joinCode,
                adminUserId = adminUser.userId,
                adminName = adminUser.displayName,
                memberUids = listOf(adminUser.userId)
            )

            val payload = mapOf(
                "familyId" to family.familyId,
                "name" to family.name,
                "joinCode" to family.joinCode,
                "adminUserId" to family.adminUserId,
                "adminName" to family.adminName,
                "memberUids" to family.memberUids,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("families").document(familyId).set(payload).await()

            // Update admin's role and familyId
            saveUser(adminUser.copy(familyId = familyId, role = UserRole.FAMILY_ADMIN.value))

            Result.success(family)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "families")
            Result.failure(e)
        }
    }

    private fun generateJoinCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { allowedChars.random() }.joinToString("")
    }

    suspend fun findFamilyByJoinCode(code: String): Result<Family?> = withContext(Dispatchers.IO) {
        try {
            val query = db.collection("families")
                .whereEqualTo("joinCode", code.trim().uppercase())
                .limit(1)
                .get()
                .await()
            val family = query.documents.firstOrNull()?.toObject(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(family)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "families?joinCode=$code")
            Result.failure(e)
        }
    }

    suspend fun updateFamilyAdmin(familyId: String, newAdminUserId: String, newAdminName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("families").document(familyId).update(
                mapOf(
                    "adminUserId" to newAdminUserId,
                    "adminName" to newAdminName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            // Update user role to Family Admin
            db.collection("users").document(newAdminUserId).update(
                mapOf(
                    "role" to UserRole.FAMILY_ADMIN.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "families/$familyId")
            Result.failure(e)
        }
    }

    suspend fun deleteFamily(familyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("families").document(familyId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "families/$familyId")
            Result.failure(e)
        }
    }

    // --- Join Requests ---
    suspend fun requestToJoinFamily(familyId: String, user: User): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestId = "req_" + UUID.randomUUID().toString().replace("-", "").take(8)
            val payload = mapOf(
                "requestId" to requestId,
                "familyId" to familyId,
                "userId" to user.userId,
                "userName" to user.displayName,
                "userEmail" to user.email,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("join_requests").document(requestId).set(payload).await()

            // Notify Admin
            sendNotification(
                AppNotification(
                    notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                    familyId = familyId,
                    recipientUserId = "family_admin",
                    title = "New Join Request",
                    message = "${user.displayName} has requested to join your family.",
                    type = "join_requested",
                    targetEntityId = requestId
                )
            )

            Result.success(requestId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "join_requests")
            Result.failure(e)
        }
    }

    fun observeJoinRequests(familyId: String): Flow<List<JoinRequest>> =
        db.collection("join_requests")
            .whereEqualTo("familyId", familyId)
            .snapshots()
            .map { it.toObjects(JoinRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "join_requests")
                throw error
            }

    suspend fun respondToJoinRequest(requestId: String, familyId: String, userId: String, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val newStatus = if (approve) "approved" else "rejected"
            db.collection("join_requests").document(requestId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (approve) {
                // Add to family memberUids
                db.collection("families").document(familyId).update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(userId),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                // Update user profile with familyId
                db.collection("users").document(userId).update(
                    mapOf(
                        "familyId" to familyId,
                        "role" to UserRole.FAMILY_MEMBER.value,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Notify user
            sendNotification(
                AppNotification(
                    notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                    familyId = familyId,
                    recipientUserId = userId,
                    title = if (approve) "Join Request Approved" else "Join Request Declined",
                    message = if (approve) "You have joined the family! Start exploring tasks." else "Your request to join the family was declined.",
                    type = if (approve) "join_approved" else "join_rejected",
                    targetEntityId = requestId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "join_requests/$requestId")
            Result.failure(e)
        }
    }

    // --- Tasks ---
    fun observeTasks(familyId: String): Flow<List<Task>> =
        db.collection("tasks")
            .whereEqualTo("familyId", familyId)
            .snapshots()
            .map { it.toObjects(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "tasks?familyId=$familyId")
                throw error
            }

    fun observeAllTasks(): Flow<List<Task>> =
        db.collection("tasks")
            .snapshots()
            .map { it.toObjects(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "tasks")
                throw error
            }

    suspend fun saveTask(task: Task, isNew: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val taskId = if (isNew && task.taskId.isBlank()) "task_" + UUID.randomUUID().toString().replace("-", "").take(8) else task.taskId
            val payload = mapOf(
                "taskId" to taskId,
                "familyId" to task.familyId,
                "title" to task.title,
                "description" to task.description,
                "points" to task.points,
                "assignedToUserId" to task.assignedToUserId,
                "assignedToUserName" to task.assignedToUserName,
                "createdByUserId" to task.createdByUserId,
                "createdByName" to task.createdByName,
                "status" to task.status,
                "scheduleType" to task.scheduleType,
                "scheduleDetails" to task.scheduleDetails,
                "isRecurring" to task.isRecurring,
                "recurringInterval" to task.recurringInterval,
                "createdAt" to (task.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("tasks").document(taskId).set(payload).await()

            // Send notification
            if (isNew) {
                if (task.status == TaskStatus.PENDING_APPROVAL.value) {
                    sendNotification(
                        AppNotification(
                            notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                            familyId = task.familyId,
                            recipientUserId = "family_admin",
                            title = "New Task Suggested",
                            message = "${task.createdByName} suggested a new task: ${task.title} (${task.points} pts)",
                            type = "task_suggested",
                            targetEntityId = taskId
                        )
                    )
                } else if (task.assignedToUserId.isNotBlank()) {
                    sendNotification(
                        AppNotification(
                            notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                            familyId = task.familyId,
                            recipientUserId = task.assignedToUserId,
                            title = "New Task Assigned",
                            message = "You have been assigned: ${task.title} for ${task.points} points.",
                            type = "task_assigned",
                            targetEntityId = taskId
                        )
                    )
                }
            } else {
                if (task.assignedToUserId.isNotBlank()) {
                    sendNotification(
                        AppNotification(
                            notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                            familyId = task.familyId,
                            recipientUserId = task.assignedToUserId,
                            title = "Task Updated",
                            message = "Task \"${task.title}\" has been updated.",
                            type = "task_changed",
                            targetEntityId = taskId
                        )
                    )
                }
            }

            Result.success(taskId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tasks")
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    suspend fun approveTaskSuggestion(task: Task, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val newStatus = if (approve) TaskStatus.ACTIVE.value else TaskStatus.REJECTED.value
            db.collection("tasks").document(task.taskId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            sendNotification(
                AppNotification(
                    notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                    familyId = task.familyId,
                    recipientUserId = task.createdByUserId,
                    title = if (approve) "Task Suggestion Approved" else "Task Suggestion Declined",
                    message = if (approve) "Your task \"${task.title}\" is now active!" else "Your suggested task was declined.",
                    type = "task_approved",
                    targetEntityId = task.taskId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/${task.taskId}")
            Result.failure(e)
        }
    }

    suspend fun completeTask(task: Task, currentUser: User, requiresAdminReview: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val status = if (requiresAdminReview && currentUser.role != UserRole.FAMILY_ADMIN.value && currentUser.role != UserRole.SUPER_ADMIN.value) {
                TaskStatus.COMPLETED_PENDING_REVIEW.value
            } else {
                TaskStatus.COMPLETED.value
            }

            db.collection("tasks").document(task.taskId).update(
                mapOf(
                    "status" to status,
                    "completedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (status == TaskStatus.COMPLETED.value) {
                // Award points immediately
                updateUserPoints(
                    userId = task.assignedToUserId,
                    newBalance = currentUser.pointsBalance + task.points,
                    totalEarnedDelta = task.points
                )
                sendNotification(
                    AppNotification(
                        notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                        familyId = task.familyId,
                        recipientUserId = task.assignedToUserId,
                        title = "Points Awarded!",
                        message = "You earned ${task.points} points for completing \"${task.title}\"!",
                        type = "task_completed",
                        targetEntityId = task.taskId
                    )
                )
            } else {
                // Notify admin for review
                sendNotification(
                    AppNotification(
                        notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                        familyId = task.familyId,
                        recipientUserId = "family_admin",
                        title = "Task Completion Review",
                        message = "${task.assignedToUserName} marked \"${task.title}\" as completed. Review required.",
                        type = "task_review_needed",
                        targetEntityId = task.taskId
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/${task.taskId}")
            Result.failure(e)
        }
    }

    suspend fun reviewTaskCompletion(task: Task, approve: Boolean, assigneeUser: User?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val status = if (approve) TaskStatus.COMPLETED.value else TaskStatus.ACTIVE.value
            db.collection("tasks").document(task.taskId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (approve && assigneeUser != null) {
                updateUserPoints(
                    userId = task.assignedToUserId,
                    newBalance = assigneeUser.pointsBalance + task.points,
                    totalEarnedDelta = task.points
                )
            }

            sendNotification(
                AppNotification(
                    notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                    familyId = task.familyId,
                    recipientUserId = task.assignedToUserId,
                    title = if (approve) "Task Completion Approved!" else "Task Needs More Work",
                    message = if (approve) "Completion of \"${task.title}\" was approved! +${task.points} points." else "Completion was not approved. Task has been reset to active.",
                    type = "task_review_result",
                    targetEntityId = task.taskId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/${task.taskId}")
            Result.failure(e)
        }
    }

    // --- Store Items ---
    fun observeStoreItems(familyId: String): Flow<List<StoreItem>> =
        db.collection("store_items")
            .whereEqualTo("familyId", familyId)
            .snapshots()
            .map { it.toObjects(StoreItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "store_items?familyId=$familyId")
                throw error
            }

    fun observeAllStoreItems(): Flow<List<StoreItem>> =
        db.collection("store_items")
            .snapshots()
            .map { it.toObjects(StoreItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "store_items")
                throw error
            }

    suspend fun saveStoreItem(item: StoreItem, isNew: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val itemId = if (isNew && item.itemId.isBlank()) "item_" + UUID.randomUUID().toString().replace("-", "").take(8) else item.itemId
            val payload = mapOf(
                "itemId" to itemId,
                "familyId" to item.familyId,
                "name" to item.name,
                "description" to item.description,
                "pointPrice" to item.pointPrice,
                "quantity" to item.quantity,
                "images" to item.images,
                "imageUrl" to item.primaryImage,
                "targetType" to item.targetType,
                "targetUserId" to item.targetUserId,
                "targetUserName" to item.targetUserName,
                "createdByUserId" to item.createdByUserId,
                "createdAt" to (item.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("store_items").document(itemId).set(payload).await()

            if (isNew) {
                // Notify relevant members
                val recipient = if (item.targetType == "specific_person") item.targetUserId else "whole_family"
                sendNotification(
                    AppNotification(
                        notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                        familyId = item.familyId,
                        recipientUserId = recipient,
                        title = "New Reward in Store!",
                        message = "\"${item.name}\" is now available for ${item.pointPrice} points (${item.quantity} available)!",
                        type = "store_item_added",
                        targetEntityId = itemId
                    )
                )
            }

            Result.success(itemId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "store_items")
            Result.failure(e)
        }
    }

    suspend fun deleteStoreItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("store_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "store_items/$itemId")
            Result.failure(e)
        }
    }

    // --- Purchases ---
    fun observePurchases(familyId: String): Flow<List<Purchase>> =
        db.collection("purchases")
            .whereEqualTo("familyId", familyId)
            .snapshots()
            .map { it.toObjects(Purchase::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "purchases?familyId=$familyId")
                throw error
            }

    fun observeAllPurchases(): Flow<List<Purchase>> =
        db.collection("purchases")
            .snapshots()
            .map { it.toObjects(Purchase::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "purchases")
                throw error
            }

    suspend fun requestPurchase(
        item: StoreItem,
        user: User,
        requestedQuantity: Int = 1
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (requestedQuantity <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Quantity must be at least 1"))
            }

            val purchaseId = "pur_" + UUID.randomUUID().toString().replace("-", "").take(8)
            var totalCost = 0
            var finalItemName = item.name

            db.runTransaction { transaction ->
                val userRef = db.collection("users").document(user.userId)
                val itemRef = db.collection("store_items").document(item.itemId)
                val purchaseRef = db.collection("purchases").document(purchaseId)

                val userSnap = transaction.get(userRef)
                val itemSnap = transaction.get(itemRef)

                if (!userSnap.exists()) {
                    throw IllegalStateException("User account not found")
                }
                if (!itemSnap.exists()) {
                    throw IllegalStateException("Store item not found")
                }

                val currentPoints = (userSnap.getLong("pointsBalance") ?: 0L).toInt()
                val availableQty = (itemSnap.getLong("quantity") ?: 0L).toInt()
                val unitPrice = (itemSnap.getLong("pointPrice") ?: item.pointPrice.toLong()).toInt()
                finalItemName = itemSnap.getString("name") ?: item.name
                val targetType = itemSnap.getString("targetType") ?: "whole_family"
                val targetUserId = itemSnap.getString("targetUserId") ?: ""

                if (targetType == "specific_person" && targetUserId.isNotBlank() && targetUserId != user.userId) {
                    throw IllegalStateException("This reward is assigned to another family member")
                }

                if (availableQty < requestedQuantity) {
                    throw IllegalStateException("Only $availableQty item(s) left in stock")
                }

                totalCost = unitPrice * requestedQuantity
                if (currentPoints < totalCost) {
                    throw IllegalStateException("Insufficient points! Required: $totalCost, Available: $currentPoints")
                }

                // 1. Immediately reserve (deduct) points from user's available balance
                val newPointsBalance = currentPoints - totalCost
                transaction.update(userRef, mapOf(
                    "pointsBalance" to newPointsBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // 2. Immediately reduce item's available quantity
                val newQty = availableQty - requestedQuantity
                transaction.update(itemRef, mapOf(
                    "quantity" to newQty,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // 3. Create purchase document
                val payload = mapOf(
                    "purchaseId" to purchaseId,
                    "familyId" to item.familyId,
                    "itemId" to item.itemId,
                    "itemName" to finalItemName,
                    "pointPrice" to unitPrice,
                    "pricePerItem" to unitPrice,
                    "quantity" to requestedQuantity,
                    "totalPoints" to totalCost,
                    "userId" to user.userId,
                    "userName" to user.displayName,
                    "status" to "pending",
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(purchaseRef, payload)
            }.await()

            // Notify Admin
            sendNotification(
                AppNotification(
                    notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                    familyId = item.familyId,
                    recipientUserId = "family_admin",
                    title = "New Purchase Request",
                    message = "${user.displayName} requested $requestedQuantity x \"$finalItemName\" ($totalCost pts reserved).",
                    type = "purchase_requested",
                    targetEntityId = purchaseId
                )
            )

            Result.success(purchaseId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "purchases")
            Result.failure(e)
        }
    }

    suspend fun cancelPurchase(purchaseId: String, currentUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var refundPoints = 0
            var refundQty = 0
            var cancelledItemName = ""
            var targetFamilyId = ""

            db.runTransaction { transaction ->
                val purchaseRef = db.collection("purchases").document(purchaseId)
                val purchaseSnap = transaction.get(purchaseRef)
                if (!purchaseSnap.exists()) {
                    throw IllegalStateException("Purchase request not found")
                }

                val status = purchaseSnap.getString("status") ?: ""
                val userId = purchaseSnap.getString("userId") ?: ""
                if (status != "pending") {
                    throw IllegalStateException("Only pending requests can be cancelled")
                }
                if (userId != currentUserId) {
                    throw IllegalStateException("You can only cancel your own purchase requests")
                }

                val itemId = purchaseSnap.getString("itemId") ?: ""
                targetFamilyId = purchaseSnap.getString("familyId") ?: ""
                cancelledItemName = purchaseSnap.getString("itemName") ?: "Reward"
                refundQty = (purchaseSnap.getLong("quantity") ?: 1L).toInt()
                refundPoints = (purchaseSnap.getLong("totalPoints") ?: purchaseSnap.getLong("pointPrice") ?: 0L).toInt()

                // Execute all READS before any WRITES (strictly required by Firestore transactions)
                val userRef = db.collection("users").document(userId)
                val userSnap = transaction.get(userRef)

                val itemRef = if (itemId.isNotBlank()) db.collection("store_items").document(itemId) else null
                val itemSnap = itemRef?.let { transaction.get(it) }

                // Now execute all WRITES:
                // 1. Mark status as cancelled
                transaction.update(purchaseRef, mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // 2. Return reserved points to user
                if (userSnap.exists()) {
                    val currentBal = (userSnap.getLong("pointsBalance") ?: 0L).toInt()
                    transaction.update(userRef, mapOf(
                        "pointsBalance" to currentBal + refundPoints,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }

                // 3. Return inventory to store item
                if (itemRef != null && itemSnap != null && itemSnap.exists()) {
                    val currentQty = (itemSnap.getLong("quantity") ?: 0L).toInt()
                    transaction.update(itemRef, mapOf(
                        "quantity" to currentQty + refundQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()

            if (targetFamilyId.isNotBlank()) {
                sendNotification(
                    AppNotification(
                        notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                        familyId = targetFamilyId,
                        recipientUserId = "family_admin",
                        title = "Purchase Cancelled",
                        message = "Purchase request for \"$cancelledItemName\" was cancelled. $refundPoints pts returned.",
                        type = "purchase_cancelled",
                        targetEntityId = purchaseId
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "purchases/$purchaseId")
            Result.failure(e)
        }
    }

    suspend fun respondToPurchase(purchase: Purchase, approve: Boolean, adminNote: String, buyerUser: User? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val newStatus = if (approve) "approved" else "rejected"
            var refundPoints = 0
            var refundQty = 0

            db.runTransaction { transaction ->
                val purchaseRef = db.collection("purchases").document(purchase.purchaseId)
                val purchaseSnap = transaction.get(purchaseRef)
                if (!purchaseSnap.exists()) {
                    throw IllegalStateException("Purchase request not found")
                }
                val currentStatus = purchaseSnap.getString("status") ?: ""
                if (currentStatus != "pending") {
                    throw IllegalStateException("Purchase is no longer pending (status: $currentStatus)")
                }

                val itemId = purchaseSnap.getString("itemId") ?: purchase.itemId
                val userId = purchaseSnap.getString("userId") ?: purchase.userId
                refundQty = (purchaseSnap.getLong("quantity") ?: purchase.quantity.toLong()).toInt()
                refundPoints = (purchaseSnap.getLong("totalPoints") ?: purchaseSnap.getLong("pointPrice") ?: purchase.pointPrice.toLong()).toInt()

                // Execute all READS before any WRITES (strictly required by Firestore transactions)
                val userRef = db.collection("users").document(userId)
                val userSnap = transaction.get(userRef)

                val itemRef = if (itemId.isNotBlank()) db.collection("store_items").document(itemId) else null
                val itemSnap = itemRef?.let { transaction.get(it) }

                // Now execute all WRITES:
                // 1. Update purchase document
                transaction.update(purchaseRef, mapOf(
                    "status" to newStatus,
                    "adminNote" to adminNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                if (approve) {
                    // Points were already reserved when requested!
                    // Increment totalPointsSpent
                    if (userSnap.exists()) {
                        val currentSpent = (userSnap.getLong("totalPointsSpent") ?: 0L).toInt()
                        transaction.update(userRef, mapOf(
                            "totalPointsSpent" to currentSpent + refundPoints,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                    }
                } else {
                    // REJECTED:
                    // 1. Return reserved points to user
                    if (userSnap.exists()) {
                        val currentBal = (userSnap.getLong("pointsBalance") ?: 0L).toInt()
                        transaction.update(userRef, mapOf(
                            "pointsBalance" to currentBal + refundPoints,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                    }

                    // 2. Return inventory quantity to store item
                    if (itemRef != null && itemSnap != null && itemSnap.exists()) {
                        val currentQty = (itemSnap.getLong("quantity") ?: 0L).toInt()
                        transaction.update(itemRef, mapOf(
                            "quantity" to currentQty + refundQty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                    }
                }
            }.await()

            sendNotification(
                AppNotification(
                    notificationId = "notif_" + UUID.randomUUID().toString().take(8),
                    familyId = purchase.familyId,
                    recipientUserId = purchase.userId,
                    title = if (approve) "Purchase Approved! 🎉" else "Purchase Declined",
                    message = if (approve) "Your purchase of \"${purchase.itemName}\" was approved and completed!" else "Your request for \"${purchase.itemName}\" was declined. $refundPoints points have been refunded.",
                    type = if (approve) "purchase_approved" else "purchase_rejected",
                    targetEntityId = purchase.purchaseId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "purchases/${purchase.purchaseId}")
            Result.failure(e)
        }
    }

    // --- Notifications ---
    fun observeNotifications(userId: String, familyId: String = ""): Flow<List<AppNotification>> =
        db.collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.recipientUserId == userId || it.recipientUserId == "whole_family" || it.recipientUserId == "family_admin" }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "notifications")
                throw error
            }

    fun observeAllNotifications(): Flow<List<AppNotification>> =
        db.collection("notifications")
            .snapshots()
            .map { it.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, "notifications")
                throw error
            }

    suspend fun sendNotification(notification: AppNotification): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val notifId = if (notification.notificationId.isBlank()) "notif_" + UUID.randomUUID().toString().take(8) else notification.notificationId
            val payload = mapOf(
                "notificationId" to notifId,
                "familyId" to notification.familyId,
                "recipientUserId" to notification.recipientUserId,
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type,
                "targetEntityId" to notification.targetEntityId,
                "isRead" to notification.isRead,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("notifications").document(notifId).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("notifications").document(notificationId).update(
                mapOf("isRead" to true)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
            Result.failure(e)
        }
    }
}
