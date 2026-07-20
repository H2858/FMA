package com.example.ui

import android.util.Log
import android.widget.Toast

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AuthScreen(viewModel: MainViewModel, onAuthSuccess: () -> Unit) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasAcceptedTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Biometric authentication dialog triggers
    var showBiometricAuthDialog by remember { mutableStateOf(false) }
    var biometricScanning by remember { mutableStateOf(false) }

    // Google Sign-In Sandbox and real flows variables
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    
    var showGoogleSandboxDialog by remember { mutableStateOf(false) }
    var googleSandboxEmail by remember { mutableStateOf("") }
    var googleSandboxName by remember { mutableStateOf("") }

    val isLiveMode by viewModel.isLiveMode.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Translation function
    fun text(key: String): String = LanguageHelper.translate(key)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background elements
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .align(Alignment.BottomCenter)
                .blur(110.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text("sync_session").uppercase(),
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "SECURE END-TO-END ENCRYPTED",
                color = EmeraldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Glassmorphic Card Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassGreenOverlay)
                    .border(
                        1.dp,
                        GlassGreenBorder,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(text("full_name"), color = TextSecondaryGreen) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_field")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(text("email_addr"), color = TextSecondaryGreen) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_field")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(text("password"), color = TextSecondaryGreen) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_field")
                    )

                    if (isSignUpMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTermsDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = hasAcceptedTerms,
                                onCheckedChange = { hasAcceptedTerms = it },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary),
                                modifier = Modifier.testTag("terms_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "أوافق على شروط الاستخدام وقسم الخصوصية",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Fields cannot be blank."
                                    return@Button
                                }
                                if (isSignUpMode && !hasAcceptedTerms) {
                                    errorMessage = "يجب عليك الموافقة على شروط الاستخدام والخصوصية للمتابعة."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                if (isSignUpMode) {
                                    viewModel.signUp(email, password, name.ifEmpty { "AI Seeker" }) { res ->
                                        isLoading = false
                                        res.fold(
                                            onSuccess = { onAuthSuccess() },
                                            onFailure = { errorMessage = it.message ?: "Authentication failed." }
                                        )
                                    }
                                } else {
                                    viewModel.login(email, password) { res ->
                                        isLoading = false
                                        res.fold(
                                            onSuccess = { onAuthSuccess() },
                                            onFailure = { errorMessage = it.message ?: "Invalid password or email." }
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_action_button")
                        ) {
                            Text(
                                text = if (isSignUpMode) text("init_access") else text("sync_secure"),
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Divider and Google Sign-In button
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            text = text("or"),
                            color = TextMutedGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // Premium Local-Simulation and Real Google Sign In option
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId("563140100900-nhvrr5igou2klj8t90isltpltkrn1d7h.apps.googleusercontent.com")
                                        .setAutoSelectEnabled(true)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        isLoading = true
                                        viewModel.loginWithGoogle(
                                            idToken = googleIdTokenCredential.idToken,
                                            email = googleIdTokenCredential.id,
                                            name = googleIdTokenCredential.displayName ?: "Google Seeker"
                                        ) { res ->
                                            isLoading = false
                                            res.fold(
                                                onSuccess = { onAuthSuccess() },
                                                onFailure = { errorMessage = it.message ?: "Google authentication failed." }
                                            )
                                        }
                                    } else {
                                        errorMessage = "Unexpected credential type returned."
                                    }
                                } catch (e: Exception) {
                                    Log.e("AuthScreen", "Google Sign In Exception: ${e.message}")
                                    // Fallback to beautiful simulator dialog as requested
                                    showGoogleSandboxDialog = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassGreenBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G ",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = text("continue_google"),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // If passkey biometric is enabled, show the high-fidelity quick biometric login trigger!
                    val isPasskeyEnrolled = userProfile?.isBiometricEnabled == true
                    if (isPasskeyEnrolled || userProfile != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val activity = context as? androidx.fragment.app.FragmentActivity
                                    if (activity != null && BiometricHelper.isBiometricAvailable(activity)) {
                                        BiometricHelper.showSystemBiometricPrompt(
                                            activity = activity,
                                            title = text("biometric_login"),
                                            subtitle = text("biometric_desc"),
                                            onSuccess = {
                                                isLoading = true
                                                val targetEmail = userProfile?.email ?: "local_sandbox@focusmate.ai"
                                                val targetName = userProfile?.name ?: "Seeker"
                                                viewModel.loginWithGoogle(
                                                    idToken = "biometric_autologin_token",
                                                    email = targetEmail,
                                                    name = targetName
                                                ) { res ->
                                                    isLoading = false
                                                    res.fold(
                                                        onSuccess = { onAuthSuccess() },
                                                        onFailure = { errorMessage = it.message }
                                                    )
                                                }
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        showBiometricAuthDialog = true
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Passkey",
                                tint = EmeraldIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = text("biometric_login").uppercase(),
                                color = EmeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Google Simulator Dialog
            if (showGoogleSandboxDialog) {
                AlertDialog(
                    onDismissRequest = { showGoogleSandboxDialog = false },
                    title = {
                        Text(
                            text = text("google_simulator"),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = text("google_sim_desc"),
                                color = TextSecondaryGreen,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = googleSandboxEmail,
                                onValueChange = { googleSandboxEmail = it },
                                label = { Text("Google Account Email", color = TextSecondaryGreen) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("google_sandbox_email")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = googleSandboxName,
                                onValueChange = { googleSandboxName = it },
                                label = { Text("Full Name", color = TextSecondaryGreen) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("google_sandbox_name")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (googleSandboxEmail.isBlank()) {
                                    errorMessage = "Google email cannot be blank."
                                    showGoogleSandboxDialog = false
                                    return@Button
                                }
                                showGoogleSandboxDialog = false
                                isLoading = true
                                viewModel.loginWithGoogle(
                                    idToken = "simulated_google_token_${UUID.randomUUID()}",
                                    email = googleSandboxEmail,
                                    name = googleSandboxName.ifEmpty { "Google Seeker" }
                                ) { res ->
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { onAuthSuccess() },
                                        onFailure = { errorMessage = it.message }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text( "auth_instantly"),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showGoogleSandboxDialog = false }) {
                            Text(
                                text("cancel"),
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.border(
                        1.dp,
                        GlassGreenBorder,
                        RoundedCornerShape(24.dp)
                    )
                )
            }

            // Biometric scan validation dialog
            if (showBiometricAuthDialog) {
                AlertDialog(
                    onDismissRequest = { showBiometricAuthDialog = false },
                    title = {
                        Text(
                            text = text("fingerprint_scan"),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = text("scan_to_authenticate"),
                                color = TextSecondaryGreen,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // High-fidelity pulsing biometric reader animation
                            val infinitePulse = rememberInfiniteTransition(label = "pulse")
                            val scale by infinitePulse.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "finger_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(if (biometricScanning) scale else 1.0f)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                                    .border(2.dp, EmeraldPrimary, CircleShape)
                                    .clickable {
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null && BiometricHelper.isBiometricAvailable(activity)) {
                                            biometricScanning = true
                                            BiometricHelper.showSystemBiometricPrompt(
                                                activity = activity,
                                                title = text("biometric_login"),
                                                subtitle = text("biometric_desc"),
                                                onSuccess = {
                                                    showBiometricAuthDialog = false
                                                    biometricScanning = false
                                                    isLoading = true
                                                    val targetEmail = userProfile?.email ?: "local_sandbox@focusmate.ai"
                                                    val targetName = userProfile?.name ?: "Seeker"
                                                    viewModel.loginWithGoogle(
                                                        idToken = "biometric_autologin_token",
                                                        email = targetEmail,
                                                        name = targetName
                                                    ) { res ->
                                                        isLoading = false
                                                        res.fold(
                                                            onSuccess = { onAuthSuccess() },
                                                            onFailure = { errorMessage = it.message }
                                                        )
                                                    }
                                                },
                                                onError = { err ->
                                                    biometricScanning = false
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            biometricScanning = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Passkey Fingerprint sensor",
                                    tint = EmeraldIconColor,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = if (biometricScanning) "VERIFYING NODE SECURE..." else "PLACE FINGER ON READER",
                                color = if (biometricScanning) EmeraldPrimary else TextSecondaryGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBiometricAuthDialog = false
                                biometricScanning = false
                                isLoading = true
                                // Log in instantly with profile credential or dummy credential in sandbox
                                val targetEmail = userProfile?.email ?: "local_sandbox@focusmate.ai"
                                val targetName = userProfile?.name ?: "Seeker"
                                viewModel.loginWithGoogle(
                                    idToken = "biometric_autologin_token",
                                    email = targetEmail,
                                    name = targetName
                                ) { res ->
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { onAuthSuccess() },
                                        onFailure = { errorMessage = it.message }
                                    )
                                }
                            },
                            enabled = biometricScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("CONFIRM VALIDATION", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showBiometricAuthDialog = false
                            biometricScanning = false
                        }) {
                            Text(text("cancel"), color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.border(1.dp, GlassGreenBorder, RoundedCornerShape(24.dp))
                )
            }

            if (showTermsDialog) {
                AlertDialog(
                    onDismissRequest = { showTermsDialog = false },
                    title = {
                        Text(
                            text = "شروط الاستخدام والخصوصية",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "يرجى قراءة شروط الاستخدام وقسم خصوصية البيانات والتدريب والموافقة عليها للمتابعة:",
                                color = TextSecondaryGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "1. سياسة استخدام البيانات:\nبموافقتك على هذه الشروط، أنت تمنحنا الحق في معالجة وتحليل البيانات التي تقدمها للتطبيق (بما في ذلك، على سبيل المثال لا الحصر: المحادثات، المهام، والتقارير اليومية).",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "2. التدريب وتحسين النموذج:\nلغرض تطوير ورفع كفاءة \"الكوتش\" وتحسين استجاباته وتخصيص تجربتك، أنت توافق صراحةً على أن بياناتك (المدخلات والمحادثات) قد تُستخدم في تدريب نماذج الذكاء الاصطناعي الخاصة بنا. نحن نهدف من خلال ذلك إلى جعل الكوتش يفهم احتياجاتك بشكل أدق ويقدم لك نصائح أكثر صرامة وواقعية بناءً على تطور أدائك.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "3. الخصوصية والسرية:\nعلى الرغم من استخدام بياناتك في عملية التدريب، فإننا نؤكد أن البيانات تُستخدم بشكل مجهول (Anonymous) وتُعالج ضمن بيئة آمنة لضمان تحسين مستوى الذكاء الاصطناعي دون الكشف عن هويتك الشخصية لأطراف خارجية.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "4. الموافقة المطلقة:\nاستخدامك للتطبيق يعد موافقة نهائية وغير مشروطة على هذه السياسة. إذا كنت لا ترغب في مشاركة بياناتك أو استخدامها في تطوير النموذج، يمكنك التوقف عن استخدام التطبيق وحذف حسابك من إعدادات النظام.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                hasAcceptedTerms = true
                                showTermsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("موافق", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTermsDialog = false }) {
                            Text("إغلاق", color = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.border(1.dp, GlassGreenBorder, RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSignUpMode) text("already_aligned") else text("new_to_app"),
                color = TextSecondaryGreen,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { isSignUpMode = !isSignUpMode }
                    .testTag("auth_mode_switch")
            )
        }
    }
}
