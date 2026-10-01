package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.model.User
import com.example.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FamTasks", appName)
    }

    @Test
    fun `user model defaults and role mapping`() {
        val user = User(
            userId = "u1",
            firstName = "John",
            secondName = "Doe",
            displayName = "John Doe",
            role = UserRole.FAMILY_ADMIN.value,
            pointsBalance = 150,
            avatarTemplate = "avatar_fox"
        )
        assertEquals("u1", user.userId)
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.secondName)
        assertEquals("avatar_fox", user.avatarTemplate)
        assertEquals(UserRole.FAMILY_ADMIN, UserRole.fromValue(user.role))
        assertEquals(150, user.pointsBalance)
        assertTrue(user.notifyNewTasks)
    }

    @Test
    fun `task model status transitions`() {
        val task = Task(
            taskId = "t1",
            title = "Clean Bedroom",
            points = 25,
            status = TaskStatus.ACTIVE.value
        )
        assertEquals(TaskStatus.ACTIVE, TaskStatus.fromValue(task.status))
        assertEquals(25, task.points)
    }
}
