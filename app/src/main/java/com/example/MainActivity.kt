package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MarketDatabase
import com.example.data.repository.MarketRepository
import com.example.model.SetupType
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockDetailScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.MarketViewModelFactory
import com.example.worker.NotificationHelper
import com.example.worker.WorkScheduler

enum class AppNavDestination(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    SCANNER("Scanner", Icons.Default.Search, "nav_scanner"),
    NEWS("News", Icons.Default.Newspaper, "nav_news"),
    JOURNAL("Journal", Icons.Default.History, "nav_journal"),
    BACKTEST("Backtest", Icons.Default.Assessment, "nav_backtest"),
    REPORTS("Reports", Icons.Default.Description, "nav_reports"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MarketViewModel by viewModels {
        val db = MarketDatabase.getInstance(applicationContext)
        val repo = MarketRepository(db)
        MarketViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize WorkManager Periodic Market Schedule & Notification Channel
        NotificationHelper.createNotificationChannel(applicationContext)
        WorkScheduler.schedulePeriodicMarketScans(applicationContext)

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MarketViewModel) {
    var currentDestination by remember { mutableStateOf(AppNavDestination.DASHBOARD) }
    var backtestInitialStrategy by remember { mutableStateOf(SetupType.BREAKOUT) }

    val selectedCandidate by viewModel.selectedCandidate.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg) {
        statusMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Handle Android system back button when in detail screen
    BackHandler(enabled = selectedCandidate != null) {
        viewModel.clearSelectedCandidate()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedCandidate == null) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Market Journal AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (selectedCandidate == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    AppNavDestination.values().forEach { dest ->
                        NavigationBarItem(
                            selected = currentDestination == dest,
                            onClick = { currentDestination = dest },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = dest.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag(dest.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        if (selectedCandidate != null) {
            StockDetailScreen(
                viewModel = viewModel,
                onBack = { viewModel.clearSelectedCandidate() },
                onNavigateToBacktest = { st ->
                    viewModel.clearSelectedCandidate()
                    backtestInitialStrategy = st
                    currentDestination = AppNavDestination.BACKTEST
                },
                modifier = modifier
            )
        } else {
            when (currentDestination) {
                AppNavDestination.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCandidate = { /* selectedCandidate handles screen change */ },
                    onNavigateToScanner = { currentDestination = AppNavDestination.SCANNER },
                    onNavigateToNews = { currentDestination = AppNavDestination.NEWS },
                    modifier = modifier
                )
                AppNavDestination.SCANNER -> ScannerScreen(
                    viewModel = viewModel,
                    onNavigateToCandidate = { /* selectedCandidate handles screen change */ },
                    modifier = modifier
                )
                AppNavDestination.NEWS -> NewsScreen(
                    viewModel = viewModel,
                    modifier = modifier
                )
                AppNavDestination.JOURNAL -> JournalScreen(
                    viewModel = viewModel,
                    modifier = modifier
                )
                AppNavDestination.BACKTEST -> BacktestScreen(
                    viewModel = viewModel,
                    initialStrategy = backtestInitialStrategy,
                    modifier = modifier
                )
                AppNavDestination.REPORTS -> ReportsScreen(
                    viewModel = viewModel,
                    modifier = modifier
                )
                AppNavDestination.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = modifier
                )
            }
        }
    }
}
