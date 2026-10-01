package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.FamTasksRepository
import com.example.localization.Strings
import com.example.model.Family
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.FamTasksLogo
import com.example.ui.components.ProfilePictureSection
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class AuthMode {
    LOGIN,
    SIGN_UP,
    GOOGLE_PROFILE_SETUP
}

@Composable
fun AuthScreen(
    repository: FamTasksRepository,
    onSuperAdminLogin: () -> Unit,
    onAuthSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fillAllFieldsMsg = Strings.get("fill_all_fields")
    val invalidCredentialsMsg = Strings.get("invalid_login_credentials")
    val invalidEmailMsg = Strings.get("invalid_email")
    val passwordMismatchMsg = Strings.get("password_mismatch")

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Google Sign-In Pending User (for New Google Account Setup)
    var pendingGoogleUser by remember { mutableStateOf<User?>(null) }

    // Login Form State
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up Form State
    var signupAvatarTemplate by remember { mutableStateOf("") }
    var signupAvatarUri by remember { mutableStateOf("") }
    var signupFirstName by remember { mutableStateOf("") }
    var signupSecondName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }
    var signupConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Google Profile Setup Form State
    var googleAvatarTemplate by remember { mutableStateOf("") }
    var googleAvatarUri by remember { mutableStateOf("") }
    var googleFirstName by remember { mutableStateOf("") }
    var googleSecondName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // FamTasks Modern Brand Logo
        FamTasksLogo(sizeDp = 68)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = Strings.get("app_title"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = Strings.get("app_tagline"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Error Banner
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        when (authMode) {
            AuthMode.LOGIN -> {
                // ==================== 1. MODERN LOGIN PAGE ====================
                Text(
                    text = Strings.get("welcome"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Email Input
                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = { loginEmail = it; errorMessage = null },
                    label = { Text(Strings.get("email")) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Input
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it; errorMessage = null },
                    label = { Text(Strings.get("password")) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(
                                imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (loginPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Main Sign In Button (PRIMARY ACTION)
                Button(
                    onClick = {
                        val trimmedEmail = loginEmail.trim().lowercase()
                        val enteredPassword = loginPassword

                        if (trimmedEmail.isBlank() || enteredPassword.isBlank()) {
                            errorMessage = fillAllFieldsMsg
                            return@Button
                        }

                        // Check Super Admin Credentials (admin@admin.com / Ehamm@123)
                        if (trimmedEmail == "admin@admin.com" && enteredPassword == "Ehamm@123") {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    Firebase.auth.signInWithEmailAndPassword(trimmedEmail, enteredPassword).await()
                                } catch (e: Exception) {
                                    try {
                                        Firebase.auth.createUserWithEmailAndPassword(trimmedEmail, enteredPassword).await()
                                    } catch (ce: Exception) {
                                        // Super Admin session ready
                                    }
                                }
                                isLoading = false
                                onSuperAdminLogin()
                            }
                            return@Button
                        }

                        // Normal User Login using Firebase Authentication
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val authResult = Firebase.auth.signInWithEmailAndPassword(trimmedEmail, enteredPassword).await()
                                val firebaseUser = authResult.user
                                if (firebaseUser == null) {
                                    isLoading = false
                                    errorMessage = invalidCredentialsMsg
                                    return@launch
                                }
                                val userResult = repository.getUserById(firebaseUser.uid)
                                val user = userResult.getOrNull() ?: User(
                                    userId = firebaseUser.uid,
                                    email = firebaseUser.email ?: trimmedEmail,
                                    displayName = firebaseUser.displayName ?: trimmedEmail.substringBefore("@")
                                )
                                isLoading = false
                                onAuthSuccess(user)
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.localizedMessage ?: invalidCredentialsMsg
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_in_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = Strings.get("sign_in"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Signup Link: "Don't have an account? Create a new account"
                // The words "new account" are the clickable highlighted link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("create_new_account_prefix"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Strings.get("new_account_link"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                errorMessage = null
                                authMode = AuthMode.SIGN_UP
                            }
                            .testTag("create_new_account_link")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Subtle Divider with small "or" label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = Strings.get("or_divider"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue with Google Button (SMALLER, SECONDARY ACTION AT BOTTOM)
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        triggerGoogleSignIn(
                            context = context,
                            repository = repository,
                            scope = scope,
                            onSuccess = { user, isNewUser ->
                                isLoading = false
                                if (isNewUser || user.firstName.isBlank()) {
                                    pendingGoogleUser = user
                                    googleFirstName = user.firstName.ifBlank { user.displayName.split(" ").firstOrNull() ?: "" }
                                    googleSecondName = user.secondName.ifBlank { user.displayName.split(" ").drop(1).joinToString(" ") }
                                    authMode = AuthMode.GOOGLE_PROFILE_SETUP
                                } else {
                                    onAuthSuccess(user)
                                }
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            onCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 16.dp)
                        .testTag("continue_with_google_button"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("continue_with_google"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            AuthMode.SIGN_UP -> {
                // ==================== 2. SIGN UP PAGE ====================
                Text(
                    text = Strings.get("create_account"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Strings.get("welcome_subtitle"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 1. Profile Picture Section (AT THE VERY TOP OF FORM)
                ProfilePictureSection(
                    selectedTemplate = signupAvatarTemplate,
                    selectedUri = signupAvatarUri,
                    onTemplateSelected = {
                        signupAvatarTemplate = it
                        signupAvatarUri = ""
                    },
                    onUriSelected = {
                        signupAvatarUri = it
                        signupAvatarTemplate = ""
                    },
                    onRemovePicture = {
                        signupAvatarTemplate = ""
                        signupAvatarUri = ""
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 2. First Name
                OutlinedTextField(
                    value = signupFirstName,
                    onValueChange = { signupFirstName = it; errorMessage = null },
                    label = { Text(Strings.get("first_name")) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_first_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Second Name
                OutlinedTextField(
                    value = signupSecondName,
                    onValueChange = { signupSecondName = it; errorMessage = null },
                    label = { Text(Strings.get("second_name")) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_second_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Email
                OutlinedTextField(
                    value = signupEmail,
                    onValueChange = { signupEmail = it; errorMessage = null },
                    label = { Text(Strings.get("email")) },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Password
                OutlinedTextField(
                    value = signupPassword,
                    onValueChange = { signupPassword = it; errorMessage = null },
                    label = { Text(Strings.get("password")) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                            Icon(
                                imageVector = if (signupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (signupPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Confirm Password
                OutlinedTextField(
                    value = signupConfirmPassword,
                    onValueChange = { signupConfirmPassword = it; errorMessage = null },
                    label = { Text(Strings.get("confirm_password")) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { signupConfirmPasswordVisible = !signupConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (signupConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (signupConfirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (signupConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_confirm_password_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 7. Create Account Button
                Button(
                    onClick = {
                        val fName = signupFirstName.trim()
                        val sName = signupSecondName.trim()
                        val email = signupEmail.trim().lowercase()

                        if (fName.isBlank() || sName.isBlank() || email.isBlank() || signupPassword.isBlank()) {
                            errorMessage = fillAllFieldsMsg
                            return@Button
                        }

                        if (!email.contains("@") || !email.contains(".")) {
                            errorMessage = invalidEmailMsg
                            return@Button
                        }

                        if (signupPassword != signupConfirmPassword) {
                            errorMessage = passwordMismatchMsg
                            return@Button
                        }

                        // Prevent overriding the Super Admin account via registration
                        if (email == "admin@admin.com") {
                            errorMessage = "Registration with this email is not permitted"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val authResult = Firebase.auth.createUserWithEmailAndPassword(email, signupPassword).await()
                                val firebaseUser = authResult.user
                                if (firebaseUser == null) {
                                    isLoading = false
                                    errorMessage = "Firebase registration returned null user"
                                    return@launch
                                }

                                val uid = firebaseUser.uid
                                val newUser = User(
                                    userId = uid,
                                    email = email,
                                    firstName = fName,
                                    secondName = sName,
                                    displayName = "$fName $sName".trim(),
                                    password = "", // Do not store plaintext password in database
                                    avatarTemplate = signupAvatarTemplate,
                                    avatarUri = signupAvatarUri,
                                    role = UserRole.FAMILY_MEMBER.value
                                )

                                val saveResult = repository.saveUser(newUser)
                                isLoading = false
                                if (saveResult.isSuccess) {
                                    onAuthSuccess(newUser)
                                } else {
                                    errorMessage = saveResult.exceptionOrNull()?.message ?: "Failed to save user profile"
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.localizedMessage ?: "Failed to create account in Firebase"
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_account_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = Strings.get("create_account"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Link back to Login
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("already_have_account_prefix"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Strings.get("sign_in_link"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                errorMessage = null
                                authMode = AuthMode.LOGIN
                            }
                            .testTag("back_to_sign_in_link")
                    )
                }
            }

            AuthMode.GOOGLE_PROFILE_SETUP -> {
                // ==================== 3. NEW GOOGLE ACCOUNT PROFILE SETUP ====================
                Text(
                    text = Strings.get("profile_setup_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Strings.get("profile_setup_subtitle"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 1. Profile Picture Section (AT THE VERY TOP)
                ProfilePictureSection(
                    selectedTemplate = googleAvatarTemplate,
                    selectedUri = googleAvatarUri,
                    onTemplateSelected = {
                        googleAvatarTemplate = it
                        googleAvatarUri = ""
                    },
                    onUriSelected = {
                        googleAvatarUri = it
                        googleAvatarTemplate = ""
                    },
                    onRemovePicture = {
                        googleAvatarTemplate = ""
                        googleAvatarUri = ""
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 2. First Name
                OutlinedTextField(
                    value = googleFirstName,
                    onValueChange = { googleFirstName = it; errorMessage = null },
                    label = { Text(Strings.get("first_name")) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_first_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Second Name
                OutlinedTextField(
                    value = googleSecondName,
                    onValueChange = { googleSecondName = it; errorMessage = null },
                    label = { Text(Strings.get("second_name")) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_second_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Continue Button
                Button(
                    onClick = {
                        val fName = googleFirstName.trim()
                        val sName = googleSecondName.trim()

                        if (fName.isBlank() || sName.isBlank()) {
                            errorMessage = fillAllFieldsMsg
                            return@Button
                        }

                        val baseUser = pendingGoogleUser ?: return@Button
                        val updatedUser = baseUser.copy(
                            firstName = fName,
                            secondName = sName,
                            displayName = "$fName $sName".trim(),
                            avatarTemplate = googleAvatarTemplate,
                            avatarUri = googleAvatarUri
                        )

                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val saveResult = repository.saveUser(updatedUser)
                            isLoading = false
                            if (saveResult.isSuccess) {
                                onAuthSuccess(updatedUser)
                            } else {
                                errorMessage = saveResult.exceptionOrNull()?.message ?: "Failed to save profile"
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_profile_continue_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = Strings.get("continue_button"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

fun triggerGoogleSignIn(
    context: Context,
    repository: FamTasksRepository,
    scope: CoroutineScope,
    onSuccess: (User, Boolean) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val credentialManager = CredentialManager.create(context)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // Check if user already exists in Firestore
                    val existingUserResult = repository.findUserByEmail(firebaseUser.email ?: "")
                    val existingUser = existingUserResult.getOrNull()

                    if (existingUser != null && existingUser.firstName.isNotBlank()) {
                        onSuccess(existingUser, false)
                    } else {
                        // Brand new user or incomplete profile
                        val splitNames = (firebaseUser.displayName ?: "").split(" ")
                        val fName = splitNames.firstOrNull() ?: ""
                        val sName = splitNames.drop(1).joinToString(" ")
                        val user = User(
                            userId = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            firstName = fName,
                            secondName = sName,
                            displayName = firebaseUser.displayName ?: "",
                            role = UserRole.FAMILY_MEMBER.value
                        )
                        onSuccess(user, true)
                    }
                } else {
                    onError("Failed to obtain Firebase user")
                }
            } else {
                onError("Unexpected credential format received")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Sign-in error")
        }
    }
}

@Composable
fun FamilyOnboardingScreen(
    currentUser: User,
    repository: FamTasksRepository,
    onFamilyJoined: (Family) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var familyName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }
    var isJoinPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FamTasksLogo(sizeDp = 64)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = Strings.get("welcome"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "${currentUser.displayName}, " + Strings.get("welcome_subtitle"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; statusMessage = null },
                text = { Text(Strings.get("create_family")) },
                icon = { Icon(Icons.Default.FamilyRestroom, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; statusMessage = null },
                text = { Text(Strings.get("join_family")) },
                icon = { Icon(Icons.Default.GroupAdd, null) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (statusMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccessMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = if (isSuccessMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (isJoinPending) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.get("request_pending"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings.get("request_pending_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (selectedTab == 0) {
            // Create Family
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = Strings.get("create_family"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You will be the Family Admin with full management permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text(Strings.get("family_name")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("family_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (familyName.isBlank()) {
                                statusMessage = "Please enter a family name"
                                isSuccessMessage = false
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val result = repository.createFamily(familyName, currentUser)
                                isLoading = false
                                if (result.isSuccess) {
                                    onFamilyJoined(result.getOrThrow())
                                } else {
                                    statusMessage = result.exceptionOrNull()?.message ?: "Failed to create family"
                                    isSuccessMessage = false
                                }
                            }
                        },
                        enabled = !isLoading && familyName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_family_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(Strings.get("create_family"))
                        }
                    }
                }
            }
        } else {
            // Join Family
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = Strings.get("join_family"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Strings.get("join_code_hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase().take(8) },
                        label = { Text(Strings.get("join_code")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_code_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (joinCode.isBlank()) {
                                statusMessage = "Please enter the join code"
                                isSuccessMessage = false
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val famResult = repository.findFamilyByJoinCode(joinCode)
                                if (famResult.isSuccess && famResult.getOrNull() != null) {
                                    val targetFam = famResult.getOrThrow()!!
                                    val reqResult = repository.requestToJoinFamily(targetFam.familyId, currentUser)
                                    isLoading = false
                                    if (reqResult.isSuccess) {
                                        isJoinPending = true
                                        statusMessage = "Join request submitted! Waiting for Family Admin approval."
                                        isSuccessMessage = true
                                    } else {
                                        statusMessage = reqResult.exceptionOrNull()?.message ?: "Failed to submit request"
                                        isSuccessMessage = false
                                    }
                                } else {
                                    isLoading = false
                                    statusMessage = "Family code not found. Please verify with your admin."
                                    isSuccessMessage = false
                                }
                            }
                        },
                        enabled = !isLoading && joinCode.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("join_family_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(Strings.get("submit_join_request"))
                        }
                    }
                }
            }
        }
    }
}
