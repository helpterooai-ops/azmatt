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

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    fun isUserLoggedIn(): Boolean {
        return try {
            auth?.currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentUid(): String? {
        return try {
            auth?.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    suspend fun registerUser(
        name: String,
        username: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        return try {
            val firebaseAuth = auth ?: throw Exception("خدمة المصادقة غير متاحة حالياً.")
            val firebaseFirestore = firestore ?: throw Exception("قاعدة البيانات غير متاحة حالياً.")

            val cleanName = name.trim()
            val cleanUsername = username.trim().removePrefix("@")
            val cleanEmail = email.trim()

            // 1. Create Auth Account in Firebase
            val authResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: throw Exception("تعذر إنشاء حساب جديد في النظام.")

            // Update Auth Profile Display Name
            try {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                user.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                // Non-fatal if updateProfile fails
            }

            val userProfile = UserProfile(
                uid = user.uid,
                name = cleanName,
                username = cleanUsername,
                email = cleanEmail,
                createdAt = System.currentTimeMillis()
            )

            // 2. Save user metadata to Firestore users collection
            try {
                firebaseFirestore.collection("users")
                    .document(user.uid)
                    .set(userProfile)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

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
            val firebaseAuth = auth ?: throw Exception("خدمة المصادقة غير متاحة حالياً.")

            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("تعذر تسجيل الدخول.")

            // Fetch profile from Firestore
            var profile = getUserProfile(user.uid)
            if (profile == null || profile.name.isBlank() || profile.name == "مستخدم أزمات") {
                val fallbackName = user.displayName?.ifBlank { null } ?: email.substringBefore("@")
                val fallbackUsername = "user_${user.uid.take(6)}"
                profile = UserProfile(
                    uid = user.uid,
                    name = fallbackName,
                    username = fallbackUsername,
                    email = user.email ?: email.trim()
                )
            }

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
            val firebaseFirestore = firestore ?: return null
            val snapshot = firebaseFirestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java)
            } else {
                val user = auth?.currentUser
                if (user != null && user.uid == uid) {
                    val fallbackName = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "مستخدم أزمات"
                    UserProfile(
                        uid = uid,
                        name = fallbackName,
                        username = "user_${uid.take(6)}",
                        email = user.email ?: ""
                    )
                } else null
            }
        } catch (e: Exception) {
            val user = auth?.currentUser
            if (user != null && user.uid == uid) {
                UserProfile(
                    uid = uid,
                    name = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "مستخدم أزمات",
                    username = "user_${uid.take(6)}",
                    email = user.email ?: ""
                )
            } else null
        }
    }

    suspend fun updateUserProfile(newName: String, newUsername: String): Result<UserProfile> {
        return try {
            val firebaseAuth = auth ?: throw Exception("خدمة المصادقة غير متاحة.")
            val currentUser = firebaseAuth.currentUser ?: throw Exception("المستخدم غير مسجل الدخول.")
            val uid = currentUser.uid

            val cleanName = newName.trim()
            val cleanUsername = newUsername.trim().removePrefix("@")

            // Update Auth display name
            try {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                currentUser.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                // Continue
            }

            val updatedProfile = UserProfile(
                uid = uid,
                name = cleanName,
                username = cleanUsername,
                email = currentUser.email ?: ""
            )

            // Update Firestore document
            try {
                firestore?.collection("users")?.document(uid)?.set(updatedProfile)?.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "فشل تحديث ملف المستخدم."))
        }
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val firebaseAuth = auth ?: throw Exception("خدمة المصادقة غير متاحة حالياً.")
            val currentUser = firebaseAuth.currentUser ?: throw Exception("لا يوجد مستخدم مسجل الدخول حالياً.")
            val uid = currentUser.uid

            // 1. Delete user profile document from Firestore
            try {
                firestore?.collection("users")?.document(uid)?.delete()?.await()
            } catch (e: Exception) {
                // Ignore if document didn't exist or offline
            }

            // 2. Delete Auth account from Firebase Authentication
            currentUser.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("requires-recent-login", ignoreCase = true)) {
                Result.failure(Exception("لحماية حسابك، يلزم تسجيل الدخول مجدداً حديثاً لإنهاء عملية حذف الحساب."))
            } else {
                Result.failure(Exception(msg.ifBlank { "حدث خطأ أثناء حذف الحساب. يرجى إعادة المحاولة." }))
            }
        }
    }
}
