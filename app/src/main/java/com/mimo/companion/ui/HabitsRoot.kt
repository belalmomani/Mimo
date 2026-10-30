package com.mimo.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mimo.companion.R

/** The "Habits" tab hosts two independent sub-screens; each works even if the other is switched off. */
@Composable
fun HabitsRoot(modifier: Modifier = Modifier) {
    var sub by remember { mutableIntStateOf(0) }
    Column(modifier.statusBarsPadding()) {
        TabRow(selectedTabIndex = sub) {
            Tab(sub == 0, { sub = 0 }, text = { Text(stringResource(R.string.sub_habits)) })
            Tab(sub == 1, { sub = 1 }, text = { Text(stringResource(R.string.sub_prayer)) })
        }
        if (sub == 0) RemindersScreen() else PrayerScreen()
    }
}
