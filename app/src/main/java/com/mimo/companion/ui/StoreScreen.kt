package com.mimo.companion.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mimo.companion.Graph
import com.mimo.companion.ads.BillingRepository
import com.mimo.companion.store.*

@Composable
fun StoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val balance by Graph.points.balance.collectAsState()
    val ownerships by Graph.storeRepo.ownerships.collectAsState()
    val activeId by Graph.storeRepo.activeShapeId.collectAsState()

    var selectedShape by remember { mutableStateOf<MimoShape?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Surface(shadowElevation = 4.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 20.sp)
                    }
                    Text("متجر Mimo", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    // Points badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⭐", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("$balance نقطة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Earn button
                Button(
                    onClick = {
                        if (activity != null) {
                            val provider = Graph.rewardedAds
                            if (provider.isReady) {
                                provider.show(
                                    activity,
                                    onRewarded = { adId ->
                                        val given = Graph.points.creditAdReward(adId)
                                        statusMessage = if (given) "🎉 ربحت ${com.mimo.companion.points.PointsConfig.POINTS_PER_AD} نقاط!"
                                        else "⚠️ تم احتساب هذا الإعلان مسبقاً"
                                    },
                                    onDismissed = { statusMessage = "أُغلق الإعلان قبل اكتمال المكافأة" },
                                    onFailed = { statusMessage = "لا يوجد إعلان متاح حالياً" }
                                )
                            } else {
                                statusMessage = "لا يوجد إعلان متاح حالياً، حاول لاحقاً"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("▶ شاهد إعلاناً واكسب نقاطاً")
                }

                AnimatedVisibility(statusMessage.isNotBlank()) {
                    Text(
                        statusMessage,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ── Grid ───────────────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Group by category
            val groups = listOf(
                "مجاني" to ShapeCatalog.all.filter { it.unlockType == UnlockType.FREE },
                "بالإعلانات" to ShapeCatalog.all.filter { it.needsAds && !it.canUsePoints },
                "مختلط" to ShapeCatalog.all.filter { it.unlockType == UnlockType.MIXED },
                "بالنقاط" to ShapeCatalog.all.filter { it.unlockType == UnlockType.POINTS }
            )
            groups.forEach { (_, shapes) ->
                items(shapes) { shape ->
                    val ownership = ownerships.find { it.shapeId == shape.id }
                    val owned = Graph.storeRepo.isOwned(shape.id)
                    ShapeCard(
                        shape = shape,
                        ownership = ownership,
                        owned = owned,
                        isActive = shape.id == activeId,
                        balance = balance,
                        onPreview = { selectedShape = shape; showPreviewDialog = true },
                        onUnlock = { selectedShape = shape; showConfirmDialog = true },
                        onSetActive = {
                            Graph.storeRepo.setActive(shape.id)
                            statusMessage = "تم تعيين ${shape.nameAr} شكلاً نشطاً ✓"
                        }
                    )
                }
            }
        }

        // ── Buy Points section ─────────────────────────────────────────────────
        if (!BillingRepository.isAvailable) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    "شراء النقاط سيكون متاحاً قريباً عبر Google Play",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ── Confirm unlock dialog ───────────────────────────────────────────────
    if (showConfirmDialog && selectedShape != null) {
        UnlockConfirmDialog(
            shape = selectedShape!!,
            balance = balance,
            ownership = ownerships.find { it.shapeId == selectedShape!!.id },
            activity = activity,
            onConfirmPoints = {
                val result = Graph.storeRepo.unlockWithPoints(selectedShape!!.id)
                statusMessage = when (result) {
                    UnlockResult.Unlocked -> "🎉 تم فتح ${selectedShape!!.nameAr}!"
                    UnlockResult.AlreadyOwned -> "أنت تملك هذا الشكل بالفعل"
                    UnlockResult.InsufficientPoints -> "رصيد غير كافٍ، شاهد إعلانات لكسب نقاط"
                    else -> "حدث خطأ"
                }
                showConfirmDialog = false
            },
            onConfirmAd = {
                val provider = Graph.rewardedAds
                if (activity != null && provider.isReady) {
                    provider.show(
                        activity,
                        onRewarded = { adId ->
                            val result = Graph.storeRepo.recordAdForShape(selectedShape!!.id, adId)
                            statusMessage = when (result) {
                                UnlockResult.Unlocked -> "🎉 تم فتح ${selectedShape!!.nameAr}!"
                                UnlockResult.AdProgressSaved -> {
                                    val o = ownerships.find { it.shapeId == selectedShape!!.id }
                                    "تقدم محفوظ ${o?.adsWatched ?: 1}/${selectedShape!!.adCount} 📈"
                                }
                                else -> "حدث خطأ"
                            }
                        },
                        onDismissed = { statusMessage = "أُغلق الإعلان قبل الإكمال" },
                        onFailed = { statusMessage = "لا يوجد إعلان" }
                    )
                } else {
                    statusMessage = "لا يوجد إعلان متاح"
                }
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    // ── Preview dialog ──────────────────────────────────────────────────────
    if (showPreviewDialog && selectedShape != null) {
        PreviewDialog(shape = selectedShape!!, onDismiss = { showPreviewDialog = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShapeCard(
    shape: MimoShape,
    ownership: ShapeOwnership?,
    owned: Boolean,
    isActive: Boolean,
    balance: Int,
    onPreview: () -> Unit,
    onUnlock: () -> Unit,
    onSetActive: () -> Unit
) {
    val rarityColor = Color(shape.rarity.color)
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else rarityColor.copy(alpha = 0.4f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isActive) 8.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onPreview)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(shape.emoji, fontSize = 36.sp)
            Text(shape.nameAr, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)

            Spacer(Modifier.height(4.dp))

            when {
                owned && isActive -> Text("✓ نشط", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                owned -> TextButton(onClick = onSetActive, contentPadding = PaddingValues(0.dp)) {
                    Text("تفعيل", fontSize = 10.sp)
                }
                shape.isFree -> Text("مجاني", fontSize = 10.sp, color = Color(0xFF4CAF50))
                else -> {
                    val label = when {
                        shape.needsAds && !shape.canUsePoints -> {
                            val watched = ownership?.adsWatched ?: 0
                            "${watched}/${shape.adCount} 📺"
                        }
                        shape.canUsePoints && !shape.needsAds -> "${shape.pointsCost} ⭐"
                        else -> "${shape.pointsCost}⭐ أو ${shape.adCount}📺"
                    }
                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onUnlock,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("فتح", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UnlockConfirmDialog(
    shape: MimoShape,
    balance: Int,
    ownership: ShapeOwnership?,
    activity: Activity?,
    onConfirmPoints: () -> Unit,
    onConfirmAd: () -> Unit,
    onDismiss: () -> Unit
) {
    val adsWatched = ownership?.adsWatched ?: 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 8.dp) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(shape.emoji, fontSize = 48.sp)
                Text(shape.nameAr, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(shape.rarity.labelAr, color = Color(shape.rarity.color), fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                // Points option
                if (shape.canUsePoints) {
                    val canAfford = balance >= shape.pointsCost
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("رصيدك: $balance ⭐", fontSize = 13.sp)
                        Text("التكلفة: ${shape.pointsCost} ⭐", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (canAfford)
                            Text("الرصيد بعد الفتح: ${balance - shape.pointsCost} ⭐", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        else
                            Text("رصيد غير كافٍ ⚠️", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onConfirmPoints, enabled = canAfford, modifier = Modifier.fillMaxWidth()) {
                            Text("فتح مقابل ${shape.pointsCost} نقطة")
                        }
                    }
                    if (shape.needsAds) {
                        Spacer(Modifier.height(8.dp))
                        Text("— أو —", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Ads option
                if (shape.needsAds) {
                    Text("التقدم: $adsWatched/${shape.adCount} إعلانات", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onConfirmAd, modifier = Modifier.fillMaxWidth()) {
                        Text("▶ شاهد إعلاناً (${adsWatched + 1}/${shape.adCount})")
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        }
    }
}

@Composable
private fun PreviewDialog(shape: MimoShape, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 8.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("معاينة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Text(shape.emoji, fontSize = 72.sp)
                Spacer(Modifier.height(8.dp))
                Text(shape.nameAr, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(shape.rarity.labelAr, color = Color(shape.rarity.color), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                val costText = when (shape.unlockType) {
                    UnlockType.FREE -> "مجاني ✓"
                    UnlockType.POINTS -> "${shape.pointsCost} نقطة"
                    UnlockType.MIXED -> "${shape.pointsCost} نقطة أو ${shape.adCount} إعلانات"
                    else -> "${shape.adCount} إعلانات"
                }
                Text(costText, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("إغلاق") }
            }
        }
    }
}
