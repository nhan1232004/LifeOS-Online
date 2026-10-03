package com.nhan.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nhan.lifeos.core.designsystem.LifeOSTheme
import com.nhan.lifeos.core.navigation.LifeOSBottomNavBar
import com.nhan.lifeos.core.navigation.Screen
import com.nhan.lifeos.ui.calendar.CalendarScreen
import com.nhan.lifeos.ui.finance.FinanceScreen
import com.nhan.lifeos.ui.more.MoreScreen
import com.nhan.lifeos.ui.today.TodayScreen
import com.nhan.lifeos.ui.todos.TodosScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeOSTheme {
                LifeOSApp()
            }
        }
    }
}

@Composable
fun LifeOSApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            LifeOSBottomNavBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) {
                TodayScreen()
            }
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }
            composable(Screen.Todos.route) {
                TodosScreen()
            }
            composable(Screen.Finance.route) {
                FinanceScreen()
            }
            composable(Screen.More.route) {
                MoreScreen()
            }
        }
    }
}
