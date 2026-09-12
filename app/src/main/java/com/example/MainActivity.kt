package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.models.RideStatus
import com.example.data.repository.RideRepository
import com.example.ui.screens.*
import com.example.ui.theme.ERide3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ERide3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ERide3App()
                }
            }
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val SELECT_USER = "select_user"

    // Passenger Routes
    const val PASSENGER_AUTH = "passenger_auth"
    const val PASSENGER_HOME = "passenger_home"
    const val PASSENGER_ACTIVE_RIDE = "passenger_active_ride"
    const val PASSENGER_HISTORY = "passenger_history"

    // Driver Routes
    const val DRIVER_AUTH = "driver_auth"
    const val DRIVER_PENDING = "driver_pending"
    const val DRIVER_HOME = "driver_home"
    const val DRIVER_ACTIVE_RIDE = "driver_active_ride"
    const val DRIVER_WALLET = "driver_wallet"
    const val DRIVER_EARNINGS = "driver_earnings"
    const val DRIVER_PROFILE = "driver_profile"
    const val DRIVER_DELETE_ACCOUNT = "driver_delete_account"

    // Admin & Help
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val HELP = "help"
}

@Composable
fun ERide3App() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Routes.SELECT_USER) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SELECT_USER) {
            SelectUserScreen(
                onSelectPassenger = {
                    navController.navigate(Routes.PASSENGER_AUTH)
                },
                onSelectDriver = {
                    navController.navigate(Routes.DRIVER_AUTH)
                },
                onSelectAdmin = {
                    navController.navigate(Routes.ADMIN_DASHBOARD)
                },
                onOpenHelp = {
                    navController.navigate(Routes.HELP)
                }
            )
        }

        // Passenger Flow
        composable(Routes.PASSENGER_AUTH) {
            PassengerAuthScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.PASSENGER_HOME) {
                        popUpTo(Routes.PASSENGER_AUTH) { inclusive = true }
                    }
                },
                onBackToPortal = {
                    navController.popBackStack(Routes.SELECT_USER, false)
                }
            )
        }

        composable(Routes.PASSENGER_HOME) {
            PassengerHomeScreen(
                onRideRequested = {
                    navController.navigate(Routes.PASSENGER_ACTIVE_RIDE)
                },
                onViewHistory = {
                    navController.navigate(Routes.PASSENGER_HISTORY)
                },
                onOpenHelp = {
                    navController.navigate(Routes.HELP)
                },
                onBackToPortal = {
                    navController.popBackStack(Routes.SELECT_USER, false)
                }
            )
        }

        composable(Routes.PASSENGER_ACTIVE_RIDE) {
            PassengerActiveRideScreen(
                onRideCompleted = {
                    navController.navigate(Routes.PASSENGER_HOME) {
                        popUpTo(Routes.PASSENGER_HOME) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.PASSENGER_HISTORY) {
            PassengerRideHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Driver Flow
        composable(Routes.DRIVER_AUTH) {
            DriverAuthScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DRIVER_HOME) {
                        popUpTo(Routes.DRIVER_AUTH) { inclusive = true }
                    }
                },
                onPendingApproval = {
                    navController.navigate(Routes.DRIVER_PENDING) {
                        popUpTo(Routes.DRIVER_AUTH) { inclusive = true }
                    }
                },
                onBackToPortal = {
                    navController.popBackStack(Routes.SELECT_USER, false)
                }
            )
        }

        composable(Routes.DRIVER_PENDING) {
            DriverPendingApprovalScreen(
                onBackToPortal = {
                    navController.popBackStack(Routes.SELECT_USER, false)
                },
                onSwitchAccount = {
                    navController.navigate(Routes.DRIVER_AUTH) {
                        popUpTo(Routes.DRIVER_PENDING) { inclusive = true }
                    }
                },
                onApproved = {
                    navController.navigate(Routes.DRIVER_HOME) {
                        popUpTo(Routes.DRIVER_PENDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DRIVER_HOME) {
            DriverHomeScreen(
                onActiveRide = {
                    navController.navigate(Routes.DRIVER_ACTIVE_RIDE)
                },
                onViewWallet = {
                    navController.navigate(Routes.DRIVER_WALLET)
                },
                onViewEarnings = {
                    navController.navigate(Routes.DRIVER_EARNINGS)
                },
                onViewProfile = {
                    navController.navigate(Routes.DRIVER_PROFILE)
                },
                onBackToPortal = {
                    navController.popBackStack(Routes.SELECT_USER, false)
                },
                onOpenAuth = {
                    navController.navigate(Routes.DRIVER_AUTH)
                }
            )
        }

        composable(Routes.DRIVER_ACTIVE_RIDE) {
            DriverActiveRideScreen(
                onRideFinished = {
                    navController.navigate(Routes.DRIVER_HOME) {
                        popUpTo(Routes.DRIVER_HOME) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.DRIVER_WALLET) {
            DriverWalletScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DRIVER_EARNINGS) {
            DriverEarningsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DRIVER_PROFILE) {
            DriverProfileScreen(
                onDeleteAccount = {
                    navController.navigate(Routes.DRIVER_DELETE_ACCOUNT)
                },
                onLogout = {
                    navController.navigate(Routes.SELECT_USER) {
                        popUpTo(Routes.SELECT_USER) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DRIVER_DELETE_ACCOUNT) {
            DriverDeleteAccountScreen(
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate(Routes.SELECT_USER) {
                        popUpTo(Routes.SELECT_USER) { inclusive = true }
                    }
                }
            )
        }

        // Admin & Help
        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onBackToPortal = {
                    navController.popBackStack(Routes.SELECT_USER, false)
                }
            )
        }

        composable(Routes.HELP) {
            HelpScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
