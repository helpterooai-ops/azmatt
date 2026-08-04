package com.example.data.repository

import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
    }

    suspend fun registerUser(
        name: String,
        username: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        return try {
            // 1. Create Auth Account in Firebase
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("تعذر إنشاء حساب جديد في النظام.")

            val userProfile = UserProfile(
                uid = user.uid,
                name = name.trim(),
                username = username.trim().removePrefix("@"),
                email = email.trim(),
                createdAt = System.currentTimeMillis()
            )

            // 2. Save user metadata to Firestore users collection
            firestore.collection("users")
                .document(user.uid)
                .set(userProfile)
                .await()

            Result.success(userProfile)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("هذا البريد الإلكتروني مُسجّل بالفعل في التطبيق! يرجى استخدام بريد آخر أو الانتقال لتسجيل الدخول."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("كلمة المرور ضعيفة جداً. يجب أن تتكون من 6 رموز أو أكثر."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("صيغة البريد الإلكتروني غير صحيحة."))
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("already in use", ignoreCase = true) || msg.contains("collision", ignoreCase = true)) {
                Result.failure(Exception("هذا البريد الإلكتروني مُسجّل بالفعل في التطبيق! لا يمكن إنشاء حساب مكرر."))
            } else {
                Result.failure(Exception(msg.ifBlank { "حدث خطأ أثناء إنشاء الحساب. يرجى إعادة المحاولة." }))
            }
        }
    }

    suspend fun loginUser(
        email: String,
        password: String
    ): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("تعذر تسجيل الدخول.")

            // Fetch profile from Firestore
            val profile = getUserProfile(user.uid) ?: UserProfile(
                uid = user.uid,
                name = user.displayName ?: "مستخدم أزمات",
                username = "user_${user.uid.take(6)}",
                email = user.email ?: email.trim()
            )

            Result.success(profile)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("الحساب غير موجود! يجب إنشاء حساب أولاً بالضغط على زر 'إنشاء حساب جديد أولاً' أدناه."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("بيانات الدخول غير صحيحة أو أن الحساب غير موجود. إذا لم يسبق لك التسجيل، اضغط على زر 'إنشاء حساب جديد أولاً' أدناه."))
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("user-not-found", ignoreCase = true) || msg.contains("no user record", ignoreCase = true)) {
                Result.failure(Exception("الحساب غير موجود! يجب إنشاء حساب أولاً بالضغط على زر 'إنشاء حساب جديد أولاً' أدناه."))
            } else {
                Result.failure(Exception(msg.ifBlank { "حدث خطأ أثناء تسجيل الدخول. يرجى التحقق من البيانات والمحاولة مجدداً." }))
            }
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        auth.signOut()
    }
}
