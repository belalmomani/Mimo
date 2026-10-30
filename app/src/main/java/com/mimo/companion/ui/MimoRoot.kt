package com.mimo.companion.ui

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MimoRoot(activity: Activity) {
    val vm: StoreViewModel = viewModel()
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        vm.messages.collect { m -> snackbar.showSnackbar(ctx.getString(m.res, *m.args.toTypedArray())) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Triple(R_home, Icons.Filled.Home, 0),
                    Triple(R_store, Icons.Filled.ShoppingCart, 1),
                    Triple(R_mine, Icons.Filled.Face, 2),
                    Triple(R_habits, Icons.Filled.Notifications, 4),
                    Triple(R_settings, Icons.Filled.Settings, 3),
                )
                items.forEach { (label, icon, idx) ->
                    NavigationBarItem(selected = tab == idx, onClick = { tab = idx },
                        icon = { Icon(icon, null) }, label = { Text(stringResource(label)) })
                }
            }
        },
    ) { pad ->
        val m = Modifier.padding(pad)
        when (tab) {
            0 -> HomeScreen(state, m)
            1 -> StoreScreen(state, vm, activity, m)
            2 -> MineScreen(state, vm, m)
            4 -> HabitsRoot(m)
            else -> SettingsScreen(m)
        }
    }
}

private val R_home = com.mimo.companion.R.string.tab_home
private val R_store = com.mimo.companion.R.string.tab_store
private val R_mine = com.mimo.companion.R.string.tab_mine
private val R_settings = com.mimo.companion.R.string.tab_settings
private val R_habits = com.mimo.companion.R.string.tab_habits
