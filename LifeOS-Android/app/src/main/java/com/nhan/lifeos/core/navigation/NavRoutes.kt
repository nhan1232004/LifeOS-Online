package com.nhan.lifeos.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Today : Screen("today", "Hôm nay", Icons.Rounded.WbSunny)
    data object Calendar : Screen("calendar", "Lịch", Icons.Rounded.CalendarMonth)
    data object Todos : Screen("todos", "Việc làm", Icons.Rounded.CheckCircle)
    data object Finance : Screen("finance", "Thu chi", Icons.Rounded.Payments)
    data object More : Screen("more", "Thêm", Icons.Rounded.GridView)

    companion object {
        val bottomNavItems = listOf(Today, Calendar, Todos, Finance, More)
    }
}
