package com.iocl.knowledgehub.domain.model

data class User(
    val uid: String = "",
    val employeeId: String = "",
    val name: String = "",
    val email: String = "",
    val department: String = "",
    val designation: String = "",
    val profileImageUrl: String = "",
    val role: UserRole = UserRole.EMPLOYEE,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    GUEST, EMPLOYEE, ADMIN, SUPER_ADMIN
}
