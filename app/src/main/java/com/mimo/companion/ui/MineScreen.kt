package com.mimo.companion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mimo.companion.R
import com.mimo.companion.domain.DEFAULT_SKIN_ID
import com.mimo.companion.domain.SkinStatus
import com.mimo.companion.face.MimoFace

/** "أشكالي": the default look + free looks + everything the user unlocked. Switching is free. */
@Composable
fun MineScreen(state: StoreState, vm: StoreViewModel, modifier: Modifier = Modifier) {
    val mine = state.items.filter { it.status is SkinStatus.Free || it.status is SkinStatus.Owned }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) { Text(stringResource(R.string.tab_mine), style = MaterialTheme.typography.headlineMedium) }
        item(key = DEFAULT_SKIN_ID) { MineCard(DEFAULT_SKIN_ID, stringResource(R.string.mine_default), state.activeSkinId == DEFAULT_SKIN_ID) { vm.activate(DEFAULT_SKIN_ID) } }
        items(mine, key = { it.def.id }) { item ->
            val name = if (java.util.Locale.getDefault().language == "ar") item.def.nameAr else item.def.nameEn
            MineCard(item.def.id, name, state.activeSkinId == item.def.id) { vm.activate(item.def.id) }
        }
        if (mine.size <= 1) item(span = { GridItemSpan(2) }) { Text(stringResource(R.string.mine_empty), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun MineCard(id: String, name: String, active: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MimoFace(id, Modifier.fillMaxWidth(), blink = false)
            Text(name, style = MaterialTheme.typography.titleSmall)
            if (active) Text(stringResource(R.string.btn_in_use), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
