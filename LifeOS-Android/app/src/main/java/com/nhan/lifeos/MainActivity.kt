package com.nhan.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSTheme
import com.nhan.lifeos.core.navigation.LifeOSBottomNavBar
import com.nhan.lifeos.core.navigation.Screen
import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.preferences.UserPreferencesRepository
import com.nhan.lifeos.data.preferences.UserSession
import com.nhan.lifeos.data.repository.CloudSyncRepository
import com.nhan.lifeos.data.repository.FinanceRepository
import com.nhan.lifeos.data.repository.MockTestRepository
import com.nhan.lifeos.data.repository.PersonalRepository
import com.nhan.lifeos.data.repository.TaskTimeRepository
import com.nhan.lifeos.data.repository.VocabRepository
import com.nhan.lifeos.ui.ai.AiScreen
import com.nhan.lifeos.ui.ai.AiViewModel
import com.nhan.lifeos.ui.mocktests.MockTestsScreen
import com.nhan.lifeos.ui.mocktests.MockTestsViewModel
import com.nhan.lifeos.ui.auth.AuthScreen
import com.nhan.lifeos.ui.calendar.CalendarScreen
import com.nhan.lifeos.ui.calendar.CalendarViewModel
import com.nhan.lifeos.ui.finance.FinanceScreen
import com.nhan.lifeos.ui.finance.FinanceViewModel
import com.nhan.lifeos.ui.goals.GoalsScreen
import com.nhan.lifeos.ui.goals.GoalsViewModel
import com.nhan.lifeos.ui.habits.HabitsScreen
import com.nhan.lifeos.ui.habits.HabitsViewModel
import com.nhan.lifeos.ui.journal.JournalScreen
import com.nhan.lifeos.ui.journal.JournalViewModel
import com.nhan.lifeos.ui.more.MoreScreen
import com.nhan.lifeos.ui.notes.NotesScreen
import com.nhan.lifeos.ui.notes.NotesViewModel
import com.nhan.lifeos.ui.pomodoro.PomodoroScreen
import com.nhan.lifeos.ui.pomodoro.PomodoroViewModel
import com.nhan.lifeos.ui.projects.ProjectsScreen
import com.nhan.lifeos.ui.projects.ProjectsViewModel
import com.nhan.lifeos.ui.settings.SettingsScreen
import com.nhan.lifeos.ui.settings.SettingsViewModel
import com.nhan.lifeos.ui.stats.StatsScreen
import com.nhan.lifeos.ui.stats.StatsViewModel
import com.nhan.lifeos.ui.today.TodayScreen
import com.nhan.lifeos.ui.today.TodayViewModel
import com.nhan.lifeos.ui.todos.TodosScreen
import com.nhan.lifeos.ui.todos.TodosViewModel
import com.nhan.lifeos.ui.vocab.VocabScreen
import com.nhan.lifeos.ui.vocab.VocabViewModel
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val preferencesRepository by lazy { UserPreferencesRepository(this) }
    private val database by lazy { LifeOSDatabase.getDatabase(this) }
    private val cloudSyncRepo by lazy { CloudSyncRepository(database, preferencesRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
        enableEdgeToEdge()
        setContent {
            LifeOSTheme {
                MainRootScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "lifeos" && uri.host == "auth-callback") {
            val uid = uri.getQueryParameter("uid") ?: return
            val email = uri.getQueryParameter("email") ?: ""
            val name = uri.getQueryParameter("name") ?: ""
            val idToken = uri.getQueryParameter("idToken") ?: ""
            val refreshToken = uri.getQueryParameter("refreshToken") ?: ""

            lifecycleScope.launch {
                preferencesRepository.setUserSession(
                    email = email,
                    displayName = name.ifBlank { email.substringBefore("@") },
                    userId = uid,
                    idToken = idToken,
                    refreshToken = refreshToken
                )
                // Trigger full sync immediately
                val userSession = preferencesRepository.userSessionFlow.first()
                cloudSyncRepo.syncAll(userSession)
            }
        }
    }
}

@Composable
fun MainRootScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    val userSessionState by preferencesRepository.userSessionFlow.collectAsState(initial = null)

    // Eagerly initialize Room DB singleton
    remember { LifeOSDatabase.getDatabase(context) }

    val session = userSessionState
    if (session == null) {
        // Initializing / Loading Splash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(LifeOSPrimary, LifeOSCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
                CircularProgressIndicator(
                    color = LifeOSPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    } else if (session.isLoggedIn) {
        // Main App Experience
        LifeOSApp(
            userSession = session,
            onSignOut = {
                coroutineScope.launch {
                    preferencesRepository.clearSession()
                }
            }
        )
    } else {
        // In-App Authentication Screen
        AuthScreen(
            onSignInSuccess = { email, name, uid, idToken, refreshToken ->
                coroutineScope.launch {
                    preferencesRepository.setUserSession(
                        email = email,
                        displayName = name,
                        userId = uid,
                        idToken = idToken,
                        refreshToken = refreshToken
                    )
                }
            },
            onContinueAsGuest = {
                coroutineScope.launch {
                    preferencesRepository.setGuestMode(true)
                }
            }
        )
    }
}

@Composable
fun LifeOSApp(
    userSession: UserSession,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { LifeOSDatabase.getDatabase(context) }
    val preferencesRepo = remember { UserPreferencesRepository(context) }
    val cloudSyncRepo = remember { CloudSyncRepository(database, preferencesRepo) }

    // Auto-sync on app launch & continuous live background sync every 30s while online
    LaunchedEffect(userSession.canSyncOnline) {
        if (userSession.canSyncOnline) {
            cloudSyncRepo.syncAll(userSession)
            while (isActive) {
                delay(30000L)
                cloudSyncRepo.syncAll(userSession)
            }
        }
    }

    val taskTimeRepo = remember { TaskTimeRepository(database, cloudSyncRepo) }
    val financeRepo = remember { FinanceRepository(database, cloudSyncRepo) }
    val personalRepo = remember { PersonalRepository(database, cloudSyncRepo) }
    val vocabRepo = remember { VocabRepository(database, cloudSyncRepo) }
    val mockTestRepo = remember { MockTestRepository(database, cloudSyncRepo) }

    val todayViewModel = remember { TodayViewModel(taskTimeRepo) }
    val todosViewModel = remember { TodosViewModel(taskTimeRepo) }
    val calendarViewModel = remember { CalendarViewModel(taskTimeRepo) }
    val projectsViewModel = remember { ProjectsViewModel(taskTimeRepo) }
    val financeViewModel = remember { FinanceViewModel(financeRepo) }
    val goalsViewModel = remember { GoalsViewModel(financeRepo) }
    val statsViewModel = remember { StatsViewModel(taskTimeRepo, financeRepo) }
    val notesViewModel = remember { NotesViewModel(personalRepo) }
    val habitsViewModel = remember { HabitsViewModel(personalRepo) }
    val journalViewModel = remember { JournalViewModel(personalRepo) }
    val pomodoroViewModel = remember { PomodoroViewModel() }
    val vocabViewModel = remember { VocabViewModel(vocabRepo) }
    val mockTestsViewModel = remember { MockTestsViewModel(mockTestRepo) }
    val aiViewModel = remember { AiViewModel(taskTimeRepo, financeRepo, personalRepo) }
    val settingsViewModel = remember { SettingsViewModel(database) }

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
                TodayScreen(viewModel = todayViewModel)
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = calendarViewModel)
            }
            composable(Screen.Todos.route) {
                TodosScreen(viewModel = todosViewModel)
            }
            composable(Screen.Finance.route) {
                FinanceScreen(viewModel = financeViewModel)
            }
            composable(Screen.More.route) {
                MoreScreen(
                    userSession = userSession,
                    cloudSyncRepo = cloudSyncRepo,
                    onSignOut = onSignOut,
                    onNavigateToFeature = { route ->
                        navController.navigate(route)
                    }
                )
            }
            composable("projects") {
                ProjectsScreen(
                    viewModel = projectsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("goals") {
                GoalsScreen(
                    viewModel = goalsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("stats") {
                StatsScreen(
                    viewModel = statsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("notes") {
                NotesScreen(
                    viewModel = notesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("habits") {
                HabitsScreen(
                    viewModel = habitsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("journal") {
                JournalScreen(
                    viewModel = journalViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("pomodoro") {
                PomodoroScreen(
                    viewModel = pomodoroViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("vocab") {
                VocabScreen(
                    viewModel = vocabViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("mocktests") {
                MockTestsScreen(
                    viewModel = mockTestsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("ai") {
                AiScreen(
                    viewModel = aiViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    cloudSyncRepo = cloudSyncRepo,
                    userSession = userSession,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
