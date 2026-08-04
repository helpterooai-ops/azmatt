package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val message: String, val profile: UserProfile? = null) : AuthUiState
    data class Error(val errorMessage: String) : AuthUiState
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _isUserLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    // Holds prefilled email & password passed after successful registration to Login screen
    private val _prefilledEmail = MutableStateFlow("")
    val prefilledEmail: StateFlow<String> = _prefilledEmail.asStateFlow()

    private val _prefilledPassword = MutableStateFlow("")
    val prefilledPassword: StateFlow<String> = _prefilledPassword.asStateFlow()

    private val _loginNoticeMessage = MutableStateFlow<String?>(null)
    val loginNoticeMessage: StateFlow<String?> = _loginNoticeMessage.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        if (repository.isUserLoggedIn()) {
            val uid = repository.getCurrentUid()
            if (uid != null) {
                viewModelScope.launch {
                    val profile = repository.getUserProfile(uid)
                    _currentUserProfile.value = profile ?: UserProfile(
                        uid = uid,
                        name = "مستخدم أزمات",
                        username = "user_${uid.take(6)}"
                    )
                    _isUserLoggedIn.value = true
                }
            } else {
                _isUserLoggedIn.value = false
            }
        } else {
            _isUserLoggedIn.value = false
        }
    }

    fun register(
        name: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccessNavToLogin: () -> Unit
    ) {
        val cleanName = name.trim()
        val cleanUsername = username.trim()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim().replace("\r", "").replace("\n", "")
        val cleanConfirmPassword = confirmPassword.trim().replace("\r", "").replace("\n", "")

        if (cleanName.isBlank() || cleanUsername.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank() || cleanConfirmPassword.isBlank()) {
            _registerState.value = AuthUiState.Error("يرجى ملء جميع الحقول المطلوبة لإنشاء الحساب.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _registerState.value = AuthUiState.Error("يرجى أدخال عنوان بريد إلكتروني صحيح (مثال: name@domain.com).")
            return
        }

        if (cleanPassword.length < 6) {
            _registerState.value = AuthUiState.Error("يجب أن تتكون كلمة المرور من 6 أحرف أو أرقام على الأقل.")
            return
        }

        if (cleanPassword != cleanConfirmPassword) {
            _registerState.value = AuthUiState.Error("كلمة المرور وتأكيد كلمة المرور غير متطابقتين.")
            return
        }

        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading
            val result = repository.registerUser(cleanName, cleanUsername, cleanEmail, cleanPassword)

            result.fold(
                onSuccess = { profile ->
                    _registerState.value = AuthUiState.Success("تم إنشاء الحساب بنجاح!")
                    // Automatically pass email and password to Login Screen
                    _prefilledEmail.value = cleanEmail
                    _prefilledPassword.value = cleanPassword
                    _loginNoticeMessage.value = "تم إنشاء الحساب بنجاح! تم تعبئة البيانات تلقائياً، اضغط على تسجيل الدخول للمتابعة."
                    
                    // Reset registration state for next use
                    _registerState.value = AuthUiState.Idle
                    onSuccessNavToLogin()
                },
                onFailure = { error ->
                    _registerState.value = AuthUiState.Error(error.localizedMessage ?: "حدث خطأ غير متوقع أثناء إنشاء الحساب.")
                }
            )
        }
    }

    fun login(
        email: String,
        password: String,
        onSuccessNavToHome: () -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim().replace("\r", "").replace("\n", "")

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            _loginState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني وكلمة المرور.")
            return
        }

        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            val result = repository.loginUser(cleanEmail, cleanPassword)

            result.fold(
                onSuccess = { profile ->
                    _currentUserProfile.value = profile
                    _isUserLoggedIn.value = true
                    _loginState.value = AuthUiState.Success("تم تسجيل الدخول بنجاح", profile)
                    _loginNoticeMessage.value = null
                    onSuccessNavToHome()
                },
                onFailure = { error ->
                    _loginState.value = AuthUiState.Error(error.localizedMessage ?: "فشل تسجيل الدخول. يرجى التحقق من صحة البيانات.")
                }
            )
        }
    }

    fun logout(onLoggedOutNavToAuth: () -> Unit) {
        repository.logout()
        _currentUserProfile.value = null
        _isUserLoggedIn.value = false
        _loginState.value = AuthUiState.Idle
        _registerState.value = AuthUiState.Idle
        _loginNoticeMessage.value = null
        onLoggedOutNavToAuth()
    }

    fun resetNoticeMessage() {
        _loginNoticeMessage.value = null
    }

    fun resetRegisterError() {
        if (_registerState.value is AuthUiState.Error) {
            _registerState.value = AuthUiState.Idle
        }
    }

    fun resetLoginError() {
        if (_loginState.value is AuthUiState.Error) {
            _loginState.value = AuthUiState.Idle
        }
    }
}
