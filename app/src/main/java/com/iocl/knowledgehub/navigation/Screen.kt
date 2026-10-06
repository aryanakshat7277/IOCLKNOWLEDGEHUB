package com.iocl.knowledgehub.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object ForgotPassword : Screen("forgot_password")

    // Main
    data object Dashboard : Screen("dashboard")
    data object Magazines : Screen("magazines")
    data object AdminNotices : Screen("admin_notices")
    data object LiveNews : Screen("live_news")
    data object LiveNewsDetail : Screen("live_news_detail/{newsId}") {
        fun createRoute(newsId: String) = "live_news_detail/$newsId"
    }
    data object Search : Screen("search")
    data object Bookmarks : Screen("bookmarks")
    data object Chatbot : Screen("chatbot")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")

    // Detail
    data object MagazineDetail : Screen("magazine_detail/{magazineId}") {
        fun createRoute(magazineId: String) = "magazine_detail/$magazineId"
    }
    data object NewspaperDetail : Screen("newspaper_detail/{newspaperId}") {
        fun createRoute(newspaperId: String) = "newspaper_detail/$newspaperId"
    }
    data object PdfViewer : Screen("pdf_viewer/{pdfUrl}/{title}") {
        fun createRoute(pdfUrl: String, title: String) =
            "pdf_viewer/${java.net.URLEncoder.encode(pdfUrl, "UTF-8")}/${java.net.URLEncoder.encode(title, "UTF-8")}"
    }

    // Admin
    data object AdminDashboard : Screen("admin_dashboard")
    data object UploadDocument : Screen("upload_document/{type}") {
        fun createRoute(type: String) = "upload_document/$type"
    }
    data object ManageUsers : Screen("manage_users")
    data object Analytics : Screen("analytics")
}
