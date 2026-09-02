package com.lockit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lockit.app.ui.TodoViewModel
import com.lockit.app.ui.screens.AddTaskScreen
import com.lockit.app.ui.screens.LockedAppsScreen
import com.lockit.app.ui.screens.TodoScreen
import com.lockit.app.ui.screens.TokenScreen
import com.lockit.app.ui.theme.LockItTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LockItTheme {
                LockItApp(viewModel)
            }
        }
    }
}

private sealed class Screen(val route: String, val label: String) {
    object Todo : Screen("todo", "Tasks")
    object Apps : Screen("apps", "Locked Apps")
    object Tokens : Screen("tokens", "Tokens")
}

@Composable
fun LockItApp(viewModel: TodoViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Todo, Screen.Apps, Screen.Tokens)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                when (screen) {
                                    Screen.Todo -> Icons.Default.CheckCircle
                                    Screen.Apps -> Icons.Default.Lock
                                    Screen.Tokens -> Icons.Default.Star
                                },
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Todo.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Screen.Todo.route) {
                TodoScreen(viewModel, onAddTask = { navController.navigate("add_task") })
            }
            composable("add_task") {
                AddTaskScreen(viewModel, onDone = { navController.popBackStack() })
            }
            composable(Screen.Apps.route) {
                LockedAppsScreen(viewModel)
            }
            composable(Screen.Tokens.route) {
                TokenScreen(viewModel)
            }
        }
    }
}
