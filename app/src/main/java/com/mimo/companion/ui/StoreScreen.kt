package com.mimo.companion.ui

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimo.companion.R
import com.mimo.companion.domain.Rarity
import com.mimo.companion.domain.SkinDef
import com.mimo.companion.domain.SkinStatus
import com.mimo.companion.domain.UnlockType
import com.mimo.companion.face.MimoFace
import com.mimo.companion.Graph
import com.mimo.companion.billing.BillingState

private fun rarityLabel(r: Rarity) = when (r) {
    Rarity.COMMON -> R.string.rarity_common; Rarity.UNCOMMON -> R.string.rarity_uncommon; Rarity.RARE -> R.string.rarity_rare
    Rarity.EPIC -> R.string.rarity_epic; Rarity.LEGENDARY -> R.string.rarity_legendary
}

@Composable
private fun skinName(d: SkinDef): String {
    val ar = java.util.Locale.getDefault().language == "ar"
    return if (ar) d.nameAr else d.nameEn
}

@Composable
fun StoreScreen(state: StoreState, vm: StoreViewModel, activity: Activity, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<String?>(null) }

    val featured = state.items.filter { it.def.featured }
    val free = state.items.filter { it.def.isFree }
    val ads = state.items.filter { it.def.allowsAds && !it.def.featured }
    val pts = state.items.filter { it.def.unlockType == UnlockType.POINTS && !it.def.featured }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2), modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) { Header(state, vm, activity) }
        section(R.string.section_featured, featured) { selected = it }
        section(R.string.section_free, free) { selected = it }
        section(R.string.section_ads, ads) { selected = it }
        section(R.string.section_points, pts) { selected = it }
        item(span = { GridItemSpan(2) }) { BuyPointsCard() }
    }

    val sel = state.items.firstOrNull { it.def.id == selected }
    if (sel != null) SkinSheet(sel, state, vm, activity) { selected = null }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(title: Int, list: List<SkinItem>, onClick: (String) -> Unit) {
    if (list.isEmpty()) return
    item(span = { GridItemSpan(2) }) { Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
    items(list, key = { it.def.id }) { SkinCard(it) { onClick(it.def.id) } }
}

@Composable
private fun Header(state: StoreState, vm: StoreViewModel, activity: Activity) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.store_title), style = MaterialTheme.typography.headlineMedium)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.points_balance, state.balance.toInt()), style = MaterialTheme.typography.titleMedium)
                Button(onClick = { vm.earnPoints(activity) }, enabled = !state.adBusy) {
                    Text(stringResource(R.string.earn_points))
                }
            }
        }
    }
}

@Composable
private fun SkinCard(item: SkinItem, onClick: () -> Unit) {
    val d = item.def
    val locked = item.status is SkinStatus.Locked || item.status is SkinStatus.InProgress
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.alpha(if (locked) 0.55f else 1f)) { MimoFace(d.id, Modifier.fillMaxWidth(), blink = false) }
            Text(skinName(d), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(stringResource(rarityLabel(d.rarity)), style = MaterialTheme.typography.labelSmall)
            Text(statusLine(item), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun statusLine(item: SkinItem): String {
    val d = item.def
    return when (val s = item.status) {
        SkinStatus.Free -> stringResource(R.string.status_free)
        SkinStatus.Owned -> stringResource(R.string.status_owned)
        is SkinStatus.InProgress -> stringResource(R.string.status_in_progress, s.done, s.total) + " · " + stringResource(R.string.ads_remaining, s.total - s.done)
        SkinStatus.Locked -> when (d.unlockType) {
            UnlockType.POINTS -> stringResource(R.string.cost_points, d.pointsCost)
            UnlockType.MIXED -> stringResource(R.string.ads_remaining, d.adsRequired) + " / " + stringResource(R.string.cost_points, d.pointsCost)
            else -> stringResource(R.string.ads_remaining, d.adsRequired)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkinSheet(item: SkinItem, state: StoreState, vm: StoreViewModel, activity: Activity, onDismiss: () -> Unit) {
    val d = item.def
    var confirm by remember { mutableStateOf(false) }
    val usable = item.status is SkinStatus.Free || item.status is SkinStatus.Owned

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MimoFace(d.id, Modifier.size(240.dp))           // preview == try: animated, free, never owned by merely previewing
            Text(skinName(d), style = MaterialTheme.typography.titleLarge)
            Text(statusLine(item), style = MaterialTheme.typography.titleSmall)
            if (!usable) Text(stringResource(R.string.trying_banner), style = MaterialTheme.typography.bodySmall)

            if (usable) {
                if (state.activeSkinId == d.id) OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(R.string.btn_in_use)) }
                else Button(onClick = { vm.activate(d.id); onDismiss() }) { Text(stringResource(R.string.btn_use)) }
            } else {
                if (d.allowsAds) {
                    Button(onClick = { vm.watchAdForSkin(activity, d.id) }, enabled = !state.adBusy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.btn_watch_for_skin, item.adsDone.coerceAtMost(d.adsRequired), d.adsRequired))
                    }
                }
                if (d.allowsPoints) {
                    Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.btn_unlock_points, d.pointsCost)) }
                }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
        }
    }

    if (confirm) {
        val after = state.balance - d.pointsCost
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.confirm_title, skinName(d))) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MimoFace(d.id, Modifier.size(120.dp), blink = false)
                    Text(stringResource(R.string.confirm_current, state.balance.toInt()))
                    Text(stringResource(R.string.confirm_cost, d.pointsCost))
                    Text(stringResource(R.string.confirm_after, maxOf(after, 0).toInt()))
                    if (after < 0) Text(stringResource(R.string.msg_not_enough, state.balance.toInt(), d.pointsCost), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                var tapped by remember { mutableStateOf(false) } // UI-level guard; the ledger is the real protection against double charges
                TextButton(enabled = after >= 0 && !tapped, onClick = { tapped = true; vm.unlockWithPoints(d.id) { confirm = false } }) { Text(stringResource(R.string.btn_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.btn_cancel)) } },
        )
    }
}

@Composable
private fun BuyPointsCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.buy_points_title), style = MaterialTheme.typography.titleMedium)
            if (Graph.billing.state == BillingState.NOT_CONFIGURED) Text(stringResource(R.string.buy_points_unavailable), style = MaterialTheme.typography.bodySmall)
        }
    }
}
