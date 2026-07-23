package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.UserRole
import com.example.ui.components.NeonButton
import com.example.ui.components.PremiumCard
import com.example.ui.theme.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    viewModel: MainViewModel,
    onLoginSuccess: (UserRole) -> Unit,
    onRegisterSuccess: (UserRole) -> Unit
) {
    val isGoogleSignInEnabled = false

    // 0 = Login, 1 = Register, 2 = Role Selection
    var currentMode by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // Form inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleEmail by remember { mutableStateOf("") }
    var googleName by remember { mutableStateOf("") }
    var isGoogleNewUser by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var selectedGoogleRole by remember { mutableStateOf<UserRole?>(null) }

    val scrollState = rememberScrollState()

    // Error states for real-time form validation
    var emailErrorState by remember { mutableStateOf<String?>(null) }
    var passwordErrorState by remember { mutableStateOf<String?>(null) }

    var firstNameErrorState by remember { mutableStateOf<String?>(null) }
    var lastNameErrorState by remember { mutableStateOf<String?>(null) }
    var registerEmailErrorState by remember { mutableStateOf<String?>(null) }
    var registerPasswordErrorState by remember { mutableStateOf<String?>(null) }
    var confirmPasswordErrorState by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = AlmostBlack,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Centered Logo
                    Image(
                        painter = painterResource(id = R.drawable.futbolcubulfoto),
                        contentDescription = "Futbolcum Logo",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Header Brand Info
                    Text(
                        text = "FUTBOLCUM’a Hoş Geldin",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Futbol dünyasındaki yerini oluşturmaya başla.",
                        color = TextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 32.dp)
                    )

                    AnimatedContent(
                        targetState = currentMode,
                        transitionSpec = {
                            slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                        },
                        label = "auth_form"
                    ) { mode ->
                        when (mode) {
                            0 -> {
                                // LOGIN MODE
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    CustomTextField(
                                        value = email,
                                        onValueChange = {
                                            email = it
                                            emailErrorState = null
                                            errorMessage = ""
                                        },
                                        label = "E-posta Adresi",
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextGray) },
                                        isError = emailErrorState != null,
                                        errorMessage = emailErrorState,
                                        testTag = "login_email_input"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    CustomTextField(
                                        value = password,
                                        onValueChange = {
                                            password = it
                                            passwordErrorState = null
                                            errorMessage = ""
                                        },
                                        label = "Şifre",
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextGray) },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = TextGray
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        isError = passwordErrorState != null,
                                        errorMessage = passwordErrorState,
                                        testTag = "login_password_input"
                                    )

                                    // Forgot Password Link - Right-aligned cyan text
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = "Şifremi Unuttum?",
                                            color = NeonCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clickable(enabled = !isLoading) {
                                                    if (email.isBlank()) {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Lütfen önce e-posta adresinizi girin.")
                                                        }
                                                    } else {
                                                        isLoading = true
                                                        viewModel.sendPasswordReset(
                                                            email = email,
                                                            onSuccess = {
                                                                isLoading = false
                                                                scope.launch {
                                                                    snackbarHostState.showSnackbar("Şifre sıfırlama bağlantısı e-postanıza gönderildi!")
                                                                }
                                                            },
                                                            onError = { err ->
                                                                isLoading = false
                                                                val mappedError = mapFirebaseError(err)
                                                                scope.launch {
                                                                    snackbarHostState.showSnackbar(mappedError)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                                .padding(vertical = 12.dp)
                                        )
                                    }

                                    if (errorMessage.isNotEmpty()) {
                                        Text(
                                            text = errorMessage,
                                            color = BrandRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // GİRİŞ YAP BUTONU
                                    PremiumAuthButton(
                                        text = if (isLoading) "GİRİŞ YAPILIYOR..." else "GİRİŞ YAP",
                                        isLoading = isLoading,
                                        onClick = {
                                            if (isLoading) return@PremiumAuthButton
                                            val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                            val isPasswordValid = password.isNotBlank()

                                            emailErrorState = if (isEmailValid) null else "Geçersiz e-posta adresi girdiniz."
                                            passwordErrorState = if (isPasswordValid) null else "Şifre alanı boş bırakılamaz."

                                            if (isEmailValid && isPasswordValid) {
                                                isLoading = true
                                                errorMessage = ""
                                                viewModel.loginUser(
                                                    email = email,
                                                    password = password,
                                                    onSuccess = { role ->
                                                        isLoading = false
                                                        onLoginSuccess(role)
                                                    },
                                                    onError = { err ->
                                                        isLoading = false
                                                        errorMessage = mapFirebaseError(err)
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(errorMessage)
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        testTag = "login_submit_button"
                                    )

                                    // Separator
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
                                        Text(
                                            text = "veya",
                                            color = TextGray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
                                    }

                                    // HESAP OLUŞTUR BUTTON
                                    OutlinedButton(
                                        onClick = { currentMode = 1; errorMessage = "" },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        border = BorderStroke(1.5.dp, NeonCyan),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = NeonCyan
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            text = "HESAP OLUŞTUR",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // TEST LOGIN - STYLED ELEGANTLY
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(NeonCyan.copy(alpha = 0.05f))
                                            .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                            .clickable {
                                                if (isLoading) return@clickable
                                                isLoading = true
                                                errorMessage = ""
                                                email = "admin@futbolcubul.com"
                                                password = "313131"
                                                viewModel.loginUser(
                                                    email = "admin@futbolcubul.com",
                                                    password = "313131",
                                                    onSuccess = { role ->
                                                        isLoading = false
                                                        onLoginSuccess(role)
                                                    },
                                                    onError = { err ->
                                                        isLoading = false
                                                        errorMessage = mapFirebaseError(err)
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(errorMessage)
                                                        }
                                                    }
                                                )
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "TEK TIKLA ADMİN GİRİŞİ ⚡",
                                                color = NeonCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // REGISTER MODE
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Hesabınızı Oluşturun",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            CustomTextField(
                                                value = firstName,
                                                onValueChange = {
                                                    firstName = it
                                                    firstNameErrorState = null
                                                },
                                                label = "Adı",
                                                isError = firstNameErrorState != null,
                                                errorMessage = firstNameErrorState,
                                                testTag = "register_first_name"
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            CustomTextField(
                                                value = lastName,
                                                onValueChange = {
                                                    lastName = it
                                                    lastNameErrorState = null
                                                },
                                                label = "Soyadı",
                                                isError = lastNameErrorState != null,
                                                errorMessage = lastNameErrorState,
                                                testTag = "register_last_name"
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    CustomTextField(
                                        value = email,
                                        onValueChange = {
                                            email = it
                                            registerEmailErrorState = null
                                        },
                                        label = "E-posta Adresi",
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextGray) },
                                        isError = registerEmailErrorState != null,
                                        errorMessage = registerEmailErrorState,
                                        testTag = "register_email"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    CustomTextField(
                                        value = password,
                                        onValueChange = {
                                            password = it
                                            registerPasswordErrorState = null
                                        },
                                        label = "Şifre",
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextGray) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        isError = registerPasswordErrorState != null,
                                        errorMessage = registerPasswordErrorState,
                                        testTag = "register_password"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    CustomTextField(
                                        value = confirmPassword,
                                        onValueChange = {
                                            confirmPassword = it
                                            confirmPasswordErrorState = null
                                        },
                                        label = "Şifreyi Onayla",
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextGray) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        isError = confirmPasswordErrorState != null,
                                        errorMessage = confirmPasswordErrorState,
                                        testTag = "register_confirm_password"
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    if (errorMessage.isNotEmpty()) {
                                        Text(
                                            text = errorMessage,
                                            color = BrandRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                    }

                                    PremiumAuthButton(
                                        text = "Sonraki: Rol Seçimi",
                                        onClick = {
                                            val isFirstNameValid = firstName.isNotBlank()
                                            val isLastNameValid = lastName.isNotBlank()
                                            val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                            val isPasswordValid = password.isNotBlank() && password.length >= 6
                                            val isConfirmValid = confirmPassword == password

                                            firstNameErrorState = if (isFirstNameValid) null else "Ad alanı boş bırakılamaz."
                                            lastNameErrorState = if (isLastNameValid) null else "Soyad alanı boş bırakılamaz."
                                            registerEmailErrorState = if (isEmailValid) null else "Geçersiz e-posta adresi."
                                            registerPasswordErrorState = if (isPasswordValid) null else "Şifre en az 6 karakter olmalıdır."
                                            confirmPasswordErrorState = if (isConfirmValid) null else "Şifreler eşleşmiyor."

                                            if (isFirstNameValid && isLastNameValid && isEmailValid && isPasswordValid && isConfirmValid) {
                                                currentMode = 2 // Go to Role Selection
                                                errorMessage = ""
                                            }
                                        },
                                        testTag = "register_submit_button"
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = "Zaten bir hesabınız var mı? ", color = TextGray, fontSize = 14.sp)
                                        Text(
                                            text = "Giriş Yap",
                                            color = NeonCyan,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { currentMode = 0; errorMessage = "" }
                                        )
                                    }
                                }
                            }

                            2 -> {
                                // ROLE SELECTION MODE
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Futbol Yolunuzu Seçin",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = "Kullanacağınız özellikleri belirlemek için rolünüzü seçin.",
                                        color = TextGray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // Player Card Option
                                    RoleCard(
                                        title = "⚽ FUTBOLCU",
                                        description = "Fiziksel istatistiklerini, biyografini ve videolarını yükle. Keşfedil ve arenalarda yer al.",
                                        icon = Icons.Default.DirectionsRun,
                                        isSelected = selectedRole == UserRole.PLAYER,
                                        onSelect = { selectedRole = UserRole.PLAYER },
                                        testTag = "role_player_card"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Scout Card Option
                                    RoleCard(
                                        title = "🕵️ SCOUT (GÖZLEMCİ)",
                                        description = "Yetenekleri teknik, taktik ve zihinsel olarak değerlendir. Resmi raporlar yaz.",
                                        icon = Icons.Default.Visibility,
                                        isSelected = selectedRole == UserRole.SCOUT,
                                        onSelect = { selectedRole = UserRole.SCOUT },
                                        testTag = "role_scout_card"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Coach Card Option
                                    RoleCard(
                                        title = "👨‍🏫 ANTRENÖR",
                                        description = "Profesyonel takımlara koçluk yap. Taktik idman programları hazırla.",
                                        icon = Icons.Default.EmojiEvents,
                                        isSelected = selectedRole == UserRole.COACH,
                                        onSelect = { selectedRole = UserRole.COACH },
                                        testTag = "role_coach_card"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Club Card Option
                                    RoleCard(
                                        title = "🏟️ KULÜP",
                                        description = "Kulübü temsil et. Altyapı akademilerini, antrenörleri ve oyuncuları koordine et.",
                                        icon = Icons.Default.Security,
                                        isSelected = selectedRole == UserRole.CLUB,
                                        onSelect = { selectedRole = UserRole.CLUB },
                                        testTag = "role_club_card"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Media Crew Card Option
                                    RoleCard(
                                        title = "🎥 MEDYA EKİBİ",
                                        description = "Maç çekimi, röportaj, drone çekimi ve profesyonel fotoğraf hizmetleri sun.",
                                        icon = Icons.Default.Videocam,
                                        isSelected = selectedRole == UserRole.MEDIA,
                                        onSelect = { selectedRole = UserRole.MEDIA },
                                        testTag = "role_media_card"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Store Card Option
                                    RoleCard(
                                        title = "🛍️ SPOR MAĞAZASI",
                                        description = "Kramponlar, formalar, antrenman yelekleri ve spor ekipmanlarını sergileyin, satış yapın.",
                                        icon = Icons.Default.Storefront,
                                        isSelected = selectedRole == UserRole.STORE,
                                        onSelect = { selectedRole = UserRole.STORE },
                                        testTag = "role_store_card"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Pitch Card Option
                                    RoleCard(
                                        title = "🏟️ HALI SAHA",
                                        description = "Halı saha tesislerinizi, çalışma saatlerini ve saha fotoğraflarını paylaşın. Rezervasyon alın.",
                                        icon = Icons.Default.Place,
                                        isSelected = selectedRole == UserRole.PITCH,
                                        onSelect = { selectedRole = UserRole.PITCH },
                                        testTag = "role_pitch_card"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Organizer Card Option
                                    RoleCard(
                                        title = "🏆 ORGANİZATÖR",
                                        description = "Ödüllü futbol turnuvaları, ligler ve dostluk müsabakaları oluşturun, yönetin.",
                                        icon = Icons.Default.DateRange,
                                        isSelected = selectedRole == UserRole.ORGANIZER,
                                        onSelect = { selectedRole = UserRole.ORGANIZER },
                                        testTag = "role_organizer_card"
                                    )

                                    Spacer(modifier = Modifier.height(28.dp))

                                    if (errorMessage.isNotEmpty()) {
                                        Text(
                                            text = errorMessage,
                                            color = BrandRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                    }

                                    PremiumAuthButton(
                                        text = if (isLoading) "Profil Hazırlanıyor..." else "Profil Sihirbazını Aç",
                                        isLoading = isLoading,
                                        onClick = {
                                            val finalRole = selectedRole
                                            if (finalRole == null) {
                                                errorMessage = "Devam etmek için lütfen bir rol seçin."
                                            } else if (isLoading) {
                                                return@PremiumAuthButton
                                            } else {
                                                isLoading = true
                                                errorMessage = ""
                                                viewModel.registerUser(
                                                    email = email,
                                                    password = password,
                                                    firstName = firstName,
                                                    lastName = lastName,
                                                    role = finalRole,
                                                    onSuccess = { role ->
                                                        isLoading = false
                                                        onRegisterSuccess(role)
                                                    },
                                                    onError = { err ->
                                                        isLoading = false
                                                        errorMessage = mapFirebaseError(err)
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(errorMessage)
                                                        }
                                                        currentMode = 1
                                                    }
                                                )
                                            }
                                        },
                                        testTag = "complete_registration_button"
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Kayda Geri Dön",
                                        color = TextGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { currentMode = 1 }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Privacy Policy & Terms of Use
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gizlilik Politikası",
                            color = TextGray,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Gizlilik Politikası yakında aktif edilecektir.")
                                }
                            }
                        )
                        Text(
                            text = "  •  ",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Kullanım Koşulları",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Kullanım Koşulları yakında aktif edilecektir.")
                                }
                            }
                        )
                    }
                }
            }
        }
    if (isGoogleSignInEnabled && showGoogleDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isGoogleLoading) {
                    showGoogleDialog = false
                    isGoogleNewUser = false
                    selectedGoogleRole = null
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = "Google",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isGoogleNewUser) "Rolünü Seç" else "Google ile Giriş Yap",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isGoogleLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    } else if (isGoogleNewUser) {
                        Text(
                            text = "Merhaba ${googleName}! Futbolcum platformunda hangi rolü üstlenmek istiyorsun? Bu seçimden sonra profil sihirbazına yönlendirileceksin.",
                            color = TextGray,
                            fontSize = 14.sp
                        )

                        // Nice, vertical choice list for roles
                        val roles = listOf(
                            UserRole.PLAYER to "Futbolcu (Oyuncu)",
                            UserRole.SCOUT to "Gözlemci (Scout)",
                            UserRole.COACH to "Antrenör / Teknik Direktör",
                            UserRole.CLUB to "Kulüp Yetkilisi",
                            UserRole.MEDIA to "Medya Ekibi",
                            UserRole.STORE to "Spor Mağazası / Store",
                            UserRole.PITCH to "Halı Saha Yetkilisi",
                            UserRole.ORGANIZER to "Turnuva & Etkinlik Organizatörü"
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            roles.forEach { (role, label) ->
                                val isSelected = selectedGoogleRole == role
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedGoogleRole = role },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSlate
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) NeonCyan else DarkBorder
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedGoogleRole = role },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = NeonCyan,
                                                unselectedColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = label, color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Lütfen devam etmek için Google hesabınızı seçin veya girin:",
                            color = TextGray,
                            fontSize = 14.sp
                        )

                        // Google Accounts suggested
                        val suggestedAccounts = listOf(
                            "jadolive38@outlook.com" to "Anıl",
                            "jadolive31@gmail.com" to "Anıl (Geliştirici)",
                            "testfutbolcu@gmail.com" to "Yusuf Demir",
                            "testscout@gmail.com" to "Sergen Yalçın",
                            "testclub@gmail.com" to "Beşiktaş JK"
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestedAccounts.forEach { (email, name) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isGoogleLoading = true
                                            googleEmail = email
                                            googleName = name
                                            viewModel.authenticateWithGoogle(
                                                email = email,
                                                displayName = name,
                                                onExistingUser = { role ->
                                                    isGoogleLoading = false
                                                    showGoogleDialog = false
                                                    onLoginSuccess(role)
                                                },
                                                onNewUser = {
                                                    isGoogleLoading = false
                                                    isGoogleNewUser = true
                                                },
                                                onError = { err ->
                                                    isGoogleLoading = false
                                                    errorMessage = err
                                                    showGoogleDialog = false
                                                }
                                            )
                                        },
                                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(NeonCyan, RoundedCornerShape(50)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name.take(1).uppercase(),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Column {
                                            Text(text = name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text(text = email, color = TextGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Veya yeni bir Google hesabı girin:",
                                color = TextGray,
                                fontSize = 12.sp
                            )

                            var customGoogleEmail by remember { mutableStateOf("") }
                            var customGoogleName by remember { mutableStateOf("") }

                            CustomTextField(
                                value = customGoogleName,
                                onValueChange = { customGoogleName = it },
                                label = "İsim Soyisim"
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            CustomTextField(
                                value = customGoogleEmail,
                                onValueChange = { customGoogleEmail = it },
                                label = "E-posta Adresi"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (customGoogleEmail.isNotBlank() && customGoogleName.isNotBlank()) {
                                        isGoogleLoading = true
                                        googleEmail = customGoogleEmail
                                        googleName = customGoogleName
                                        viewModel.authenticateWithGoogle(
                                            email = customGoogleEmail,
                                            displayName = customGoogleName,
                                            onExistingUser = { role ->
                                                isGoogleLoading = false
                                                showGoogleDialog = false
                                                onLoginSuccess(role)
                                            },
                                            onNewUser = {
                                                isGoogleLoading = false
                                                isGoogleNewUser = true
                                            },
                                            onError = { err ->
                                                isGoogleLoading = false
                                                errorMessage = err
                                                showGoogleDialog = false
                                            }
                                        )
                                    }
                                },
                                enabled = customGoogleEmail.isNotBlank() && customGoogleName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Giriş Yap", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isGoogleNewUser && !isGoogleLoading) {
                    Button(
                        onClick = {
                            val role = selectedGoogleRole
                            if (role != null) {
                                isGoogleLoading = true
                                viewModel.completeGoogleRegistration(
                                    email = googleEmail,
                                    displayName = googleName,
                                    role = role,
                                    onSuccess = { completedRole ->
                                        isGoogleLoading = false
                                        showGoogleDialog = false
                                        isGoogleNewUser = false
                                        onRegisterSuccess(completedRole)
                                    },
                                    onError = { err ->
                                        isGoogleLoading = false
                                        errorMessage = err
                                        showGoogleDialog = false
                                        isGoogleNewUser = false
                                    }
                                )
                            }
                        },
                        enabled = selectedGoogleRole != null,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Sihirbazı Başlat", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isGoogleLoading) {
                    TextButton(
                        onClick = {
                            showGoogleDialog = false
                            isGoogleNewUser = false
                            selectedGoogleRole = null
                        }
                    ) {
                        Text("Vazgeç", color = Color.White)
                    }
                }
            },
            containerColor = AlmostBlack,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isError -> BrandRed
        isFocused -> NeonCyan
        else -> Color(0xFF2E2E38) // Koyu gri çerçeve
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused && !isError) 0.12f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "glow"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            Text(
                text = label.uppercase(),
                color = if (isFocused) NeonCyan else Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF14161D)) // Koyu gri dolgu
                .border(
                    BorderStroke(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = borderColor
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .then(
                    if (glowAlpha > 0f) {
                        Modifier.border(
                            BorderStroke(4.dp, NeonCyan.copy(alpha = glowAlpha)),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            val displayPlaceholder = when {
                placeholder.isNotBlank() -> placeholder
                label.contains("(") -> label.substringAfter("(").substringBefore(")").trim()
                else -> ""
            }

            TextField(
                value = value,
                onValueChange = { input ->
                    if (keyboardOptions.keyboardType == KeyboardType.Number) {
                        val cleaned = input.filter { it.isDigit() }
                        onValueChange(cleaned)
                    } else {
                        onValueChange(input)
                    }
                },
                placeholder = {
                    if (displayPlaceholder.isNotBlank()) {
                        Text(
                            text = displayPlaceholder,
                            color = TextGray.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                singleLine = true,
                interactionSource = interactionSource,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = NeonCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            )
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = BrandRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PremiumAuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    testTag: String = ""
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            disabledContainerColor = NeonCyan.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        enabled = !isLoading,
        contentPadding = PaddingValues()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = AlmostBlack,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                color = AlmostBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

fun mapFirebaseError(error: String): String {
    val errLower = error.lowercase()
    return when {
        errLower.contains("user-not-found") || errLower.contains("no user record") || errLower.contains("kullanıcı bulunamadı") -> 
            "E-posta adresine kayıtlı kullanıcı bulunamadı."
        errLower.contains("wrong-password") || errLower.contains("invalid-credential") || errLower.contains("yanlış şifre") -> 
            "E-posta adresi veya şifre hatalı."
        errLower.contains("invalid-email") || errLower.contains("geçersiz e-posta") -> 
            "Lütfen geçerli bir e-posta adresi girin."
        errLower.contains("too-many-requests") || errLower.contains("çok fazla deneme") -> 
            "Çok fazla başarısız deneme yapıldı. Lütfen daha sonra tekrar deneyin."
        errLower.contains("network-request-failed") || errLower.contains("bağlantı hatası") || errLower.contains("network error") -> 
            "İnternet bağlantısı kurulamadı. Lütfen ağınızı kontrol edin."
        errLower.contains("email-already-in-use") || errLower.contains("already exists") -> 
            "Bu e-posta adresi zaten kullanımda."
        errLower.contains("weak-password") -> 
            "Şifre çok zayıf. Lütfen daha güçlü bir şifre belirleyin."
        else -> error
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    testTag: String
) {
    PremiumCard(
        borderGlow = isSelected,
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) NeonCyan else DarkBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AlmostBlack else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) NeonCyan else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
