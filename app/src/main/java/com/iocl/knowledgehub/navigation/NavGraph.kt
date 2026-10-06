package com.iocl.knowledgehub.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.iocl.knowledgehub.ui.admin.AdminDashboardScreen
import com.iocl.knowledgehub.ui.admin.AnalyticsScreen
import com.iocl.knowledgehub.ui.admin.ManageUsersScreen
import com.iocl.knowledgehub.ui.admin.UploadDocumentScreen
import com.iocl.knowledgehub.ui.auth.ForgotPasswordScreen
import com.iocl.knowledgehub.ui.auth.LoginScreen
import com.iocl.knowledgehub.ui.bookmark.BookmarkScreen
import com.iocl.knowledgehub.ui.chatbot.ChatbotScreen
import com.iocl.knowledgehub.ui.dashboard.DashboardScreen

import com.iocl.knowledgehub.ui.magazine.MagazineDetailScreen
import com.iocl.knowledgehub.ui.magazine.MagazineListScreen
import com.iocl.knowledgehub.ui.newspaper.NewspaperDetailScreen
import com.iocl.knowledgehub.ui.newspaper.NewspaperListScreen
import com.iocl.knowledgehub.ui.livenews.LiveNewsScreen
import com.iocl.knowledgehub.ui.livenews.LiveNewsDetailScreen
import com.iocl.knowledgehub.ui.notification.NotificationScreen
import com.iocl.knowledgehub.ui.pdfviewer.PdfViewerScreen
import com.iocl.knowledgehub.ui.profile.ProfileScreen
import com.iocl.knowledgehub.ui.search.SearchScreen
import com.iocl.knowledgehub.domain.model.DocumentType
import com.iocl.knowledgehub.domain.model.UserRole

private const val TRANSITION_DURATION = 350

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        }
    ) {
        // ==================== AUTH ====================
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // ==================== MAIN ====================
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToMagazines = { navController.navigate(Screen.Magazines.route) },
                onNavigateToAdminNotices = { navController.navigate(Screen.AdminNotices.route) },
                onNavigateToLiveNews = { navController.navigate(Screen.LiveNews.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                onNavigateToChatbot = { navController.navigate(Screen.Chatbot.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                onNavigateToMagazineDetail = { id ->
                    navController.navigate(Screen.MagazineDetail.createRoute(id))
                },
                onNavigateToNewspaperDetail = { id ->
                    navController.navigate(Screen.NewspaperDetail.createRoute(id))
                }
            )
        }



        composable(Screen.Magazines.route) {
            MagazineListScreen(
                onBackClick = { navController.popBackStack() },
                onMagazineClick = { id ->
                    navController.navigate(Screen.MagazineDetail.createRoute(id))
                },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onUploadMagazineClick = { navController.navigate(Screen.UploadDocument.createRoute("Magazine")) }
            )
        }

        composable(Screen.AdminNotices.route) {
            NewspaperListScreen(
                onBackClick = { navController.popBackStack() },
                onNewspaperClick = { id ->
                    navController.navigate(Screen.NewspaperDetail.createRoute(id))
                },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onUploadClick = { navController.navigate(Screen.UploadDocument.createRoute("Newspaper")) }
            )
        }

        composable(Screen.LiveNews.route) {
            LiveNewsScreen(
                onBackClick = { navController.popBackStack() },
                onNewsClick = { id ->
                    navController.navigate(Screen.LiveNewsDetail.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.LiveNewsDetail.route,
            arguments = listOf(
                navArgument("newsId") { type = NavType.StringType }
            )
        ) {
            LiveNewsDetailScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onMagazineClick = { id ->
                    navController.navigate(Screen.MagazineDetail.createRoute(id))
                },
                onNewspaperClick = { id ->
                    navController.navigate(Screen.NewspaperDetail.createRoute(id))
                }
            )
        }

        composable(Screen.Bookmarks.route) {
            BookmarkScreen(
                onBackClick = { navController.popBackStack() },
                onMagazineClick = { id ->
                    navController.navigate(Screen.MagazineDetail.createRoute(id))
                },
                onNewspaperClick = { id ->
                    navController.navigate(Screen.NewspaperDetail.createRoute(id))
                }
            )
        }

        composable(Screen.Chatbot.route) {
            ChatbotScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationScreen(
                onBackClick = { navController.popBackStack() },
                onNotificationClick = { notification ->
                    notification.documentId?.let { docId ->
                        if (notification.documentType == DocumentType.MAGAZINE) {
                            navController.navigate(Screen.MagazineDetail.createRoute(docId))
                        } else {
                            navController.navigate(Screen.NewspaperDetail.createRoute(docId))
                        }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ==================== DETAIL ====================
        composable(
            route = Screen.MagazineDetail.route,
            arguments = listOf(navArgument("magazineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val magazineId = backStackEntry.arguments?.getString("magazineId") ?: ""
            MagazineDetailScreen(
                magazineId = magazineId,
                onBackClick = { navController.popBackStack() },
                onOpenPdf = { pdfUrl, title ->
                    navController.navigate(Screen.PdfViewer.createRoute(pdfUrl, title))
                }
            )
        }

        composable(
            route = Screen.NewspaperDetail.route,
            arguments = listOf(navArgument("newspaperId") { type = NavType.StringType })
        ) { backStackEntry ->
            val newspaperId = backStackEntry.arguments?.getString("newspaperId") ?: ""
            NewspaperDetailScreen(
                newspaperId = newspaperId,
                onBackClick = { navController.popBackStack() },
                onOpenPdf = { pdfUrl, title ->
                    navController.navigate(Screen.PdfViewer.createRoute(pdfUrl, title))
                }
            )
        }

        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("pdfUrl") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pdfUrl = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("pdfUrl") ?: "", "UTF-8"
            )
            val title = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("title") ?: "", "UTF-8"
            )
            PdfViewerScreen(
                pdfUrl = pdfUrl,
                title = title,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ==================== ADMIN ====================
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onUploadClick = { navController.navigate(Screen.UploadDocument.route) },
                onManageUsersClick = { navController.navigate(Screen.ManageUsers.route) },
                onAnalyticsClick = { navController.navigate(Screen.Analytics.route) }
            )
        }

        composable(Screen.UploadDocument.route) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "Magazine"
            UploadDocumentScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageUsers.route) {
            ManageUsersScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
