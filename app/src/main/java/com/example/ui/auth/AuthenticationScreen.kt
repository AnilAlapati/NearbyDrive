package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NearbyDriveViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    appViewModel: NearbyDriveViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }
    
    // Inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    
    // UI states
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    
    // Main Container Gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            OceanLight,
            DarkBlueCard
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Header Section
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(OceanBlue.copy(alpha = 0.15f))
                    .border(2.dp, OceanBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = "Logo",
                    tint = OceanBlue,
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NearbyDrive",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = SlateDark,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Secure Peer-to-Peer Neighborhood Electric & Gas Rides Matching",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateBlueText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Auth Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftGray)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isSignUp) Color.White else Color.Transparent)
                        .clickable { 
                            isSignUp = false
                            errorMessage = null
                        }
                        .testTag("auth_tab_login"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.Bold,
                        color = if (!isSignUp) OceanBlue else SlateBlueText
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSignUp) Color.White else Color.Transparent)
                        .clickable { 
                            isSignUp = true
                            errorMessage = null
                        }
                        .testTag("auth_tab_signup"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        fontWeight = FontWeight.Bold,
                        color = if (isSignUp) OceanBlue else SlateBlueText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )

                    // Profile Full Name for SignUp
                    AnimatedVisibility(
                        visible = isSignUp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = OceanBlue) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OceanBlue,
                                unfocusedBorderColor = SoftGray
                            )
                        )
                    }

                    // Email Address Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = OceanBlue) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = SoftGray
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = OceanBlue) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = SlateBlueText
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = SoftGray
                        )
                    )

                    // Error display
                    errorMessage?.let { msg ->
                        Text(
                            text = "⚠️ $msg",
                            color = AccentCoral,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Success display
                    successMessage?.let { msg ->
                        Text(
                            text = "✅ $msg",
                            color = MintGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Core Button
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            if (isSignUp) {
                                appViewModel.handleSignUp(
                                    email = email,
                                    password = password,
                                    fullName = fullName,
                                    onSuccess = {
                                        isLoading = false
                                        successMessage = "Account created successfully!"
                                    },
                                    onFailure = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            } else {
                                appViewModel.handleLogin(
                                    email = email,
                                    password = password,
                                    onSuccess = {
                                        isLoading = false
                                        successMessage = "Signed in successfully!"
                                    },
                                    onFailure = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isSignUp) "Create Account" else "Sign In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social divider
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBlueText.copy(alpha = 0.2f))
                Text(
                    text = "or continue with",
                    color = SlateBlueText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBlueText.copy(alpha = 0.2f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Google sign in button
            OutlinedButton(
                onClick = { showGoogleDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_google_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, SoftGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateDark, containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Custom Google Mini Vector icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White)
                    ) {
                        Text("G", color = OceanBlue, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Helpful guide about Firebase Secrets integration
            Text(
                text = "✨ Pro Tip: If Firebase Credentials are not configured in your AI Studio Secrets panel, the app operates in an fully-interactive offline simulation container out of the box!",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = SlateBlueText.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Google Sign-In interactive popup dialog
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", color = OceanBlue, fontWeight = FontWeight.Black, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, color = SlateDark)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Choose an account to continue to NearbyDrive:",
                        fontSize = 14.sp,
                        color = SlateBlueText
                    )

                    // Auto-detect & offer user registered email 
                    val userEmailSuggestion = "anilalapati.66@gmail.com"
                    
                    // Selected google email options
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Env User
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(OceanLight)
                                .clickable {
                                    showGoogleDialog = false
                                    isLoading = true
                                    appViewModel.handleGoogleLogin(
                                        userEmailSuggestion,
                                        onSuccess = { isLoading = false },
                                        onFailure = { isLoading = false }
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OceanBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    userEmailSuggestion.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Anil Alapati", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(userEmailSuggestion, fontSize = 12.sp, color = SlateBlueText)
                            }
                        }

                        // Option 2: Default Resident fallback
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftGray)
                                .clickable {
                                    showGoogleDialog = false
                                    isLoading = true
                                    appViewModel.handleGoogleLogin(
                                        "rohan@society.com",
                                        onSuccess = { isLoading = false },
                                        onFailure = { isLoading = false }
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SlateBlueText),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("R", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Rohan Sharma", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("rohan@society.com", fontSize = 12.sp, color = SlateBlueText)
                            }
                        }
                    }

                    // Direct input custom email options
                    var customGoogleEmail by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = customGoogleEmail,
                        onValueChange = { customGoogleEmail = it },
                        label = { Text("Use another Google account email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_google_email_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = SoftGray
                        )
                    )

                    Button(
                        onClick = {
                            if (customGoogleEmail.contains("@")) {
                                showGoogleDialog = false
                                isLoading = true
                                appViewModel.handleGoogleLogin(
                                    customGoogleEmail,
                                    onSuccess = { isLoading = false },
                                    onFailure = { isLoading = false }
                                )
                            }
                        },
                        enabled = customGoogleEmail.contains("@"),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Selection", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancel", color = SlateBlueText)
                }
            }
        )
    }
}
