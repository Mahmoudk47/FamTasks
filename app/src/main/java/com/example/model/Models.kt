package com.example.model

import com.google.firebase.Timestamp

enum class UserRole(val value: String) {
    SUPER_ADMIN("super_admin"),
    FAMILY_ADMIN("family_admin"),
    FAMILY_MEMBER("family_member");

    companion object {
        fun fromValue(value: String): UserRole = when (value) {
            "super_admin" -> SUPER_ADMIN
            "family_admin" -> FAMILY_ADMIN
            else -> FAMILY_MEMBER
        }
    }
}

data class User(
    val userId: String = "",
    val email: String = "",
    val firstName: String = "",
    val secondName: String = "",
    val displayName: String = "",
    val password: String = "",
    val avatarTemplate: String = "",
    val avatarUri: String = "",
    val role: String = UserRole.FAMILY_MEMBER.value,
    val familyId: String = "",
    val pointsBalance: Int = 0,
    val totalPointsEarned: Int = 0,
    val totalPointsSpent: Int = 0,
    val notifyNewTasks: Boolean = true,
    val notifyTaskChanges: Boolean = true,
    val notifyStoreItems: Boolean = true,
    val notifyPurchases: Boolean = true,
    val notifyApprovals: Boolean = true,
    val preferredLanguage: String = "en",
    val themeMode: String = "system",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Family(
    val familyId: String = "",
    val name: String = "",
    val joinCode: String = "",
    val adminUserId: String = "",
    val adminName: String = "",
    val memberUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class JoinRequest(
    val requestId: String = "",
    val familyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val status: String = "pending", // pending, approved, rejected
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

enum class TaskStatus(val value: String) {
    PENDING_APPROVAL("pending_approval"),
    ACTIVE("active"),
    COMPLETED_PENDING_REVIEW("completed_pending_review"),
    COMPLETED("completed"),
    REJECTED("rejected");

    companion object {
        fun fromValue(value: String): TaskStatus = when (value) {
            "pending_approval" -> PENDING_APPROVAL
            "active" -> ACTIVE
            "completed_pending_review" -> COMPLETED_PENDING_REVIEW
            "completed" -> COMPLETED
            "rejected" -> REJECTED
            else -> ACTIVE
        }
    }
}

data class Task(
    val taskId: String = "",
    val familyId: String = "",
    val title: String = "",
    val description: String = "",
    val points: Int = 10,
    val assignedToUserId: String = "",
    val assignedToUserName: String = "",
    val createdByUserId: String = "",
    val createdByName: String = "",
    val status: String = TaskStatus.ACTIVE.value,
    val scheduleType: String = "one_time", // one_time, scheduled, daily, weekly, custom
    val scheduleDetails: String = "",
    val isRecurring: Boolean = false,
    val recurringInterval: String = "none", // none, daily, weekly, monthly
    val completedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class StoreItem(
    val itemId: String = "",
    val familyId: String = "",
    val name: String = "",
    val description: String = "",
    val pointPrice: Int = 50,
    val quantity: Int = 10,
    val images: List<String> = emptyList(),
    val imageUrl: String = "",
    val targetType: String = "whole_family", // whole_family, specific_person
    val targetUserId: String = "",
    val targetUserName: String = "",
    val createdByUserId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val primaryImage: String
        get() = images.firstOrNull() ?: imageUrl
}

data class Purchase(
    val purchaseId: String = "",
    val familyId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val pointPrice: Int = 0,
    val pricePerItem: Int = 0,
    val quantity: Int = 1,
    val totalPoints: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val status: String = "pending", // pending, approved, rejected, cancelled
    val adminNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class AppNotification(
    val notificationId: String = "",
    val familyId: String = "",
    val recipientUserId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "task",
    val targetEntityId: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
