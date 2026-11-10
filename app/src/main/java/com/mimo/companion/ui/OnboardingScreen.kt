package com.mimo.companion.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimo.companion.Graph
import com.mimo.companion.domain.dialogue.Lang
import com.mimo.companion.domain.prayer.Cities
import com.mimo.companion.domain.prayer.City

/**
 * First-run wizard: 2 steps.
 *   Step 0 — choose language (AR / EN)
 *   Step 1 — choose home city for prayer times (can skip)
 *
 * On completion calls [onDone]; the caller (MainActivity) persists the flag
 * and recreates the Activity so the chosen language loads immediately.
 */
@Composable
fun OnboardingScreen(onDone: (lang: Lang, cityId: String?) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var chosenLang by remember { mutableStateOf(Lang.AR) }
    val totalSteps = 2

    Column(Modifier.fillMaxSize()) {
        // Progress bar
        LinearProgressIndicator(
            progress = { (step + 1).toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            label = "onboarding_step",
        ) { currentStep ->
            when (currentStep) {
                0 -> LangStep(
                    selected = chosenLang,
                    onSelect = { chosenLang = it },
                    onNext = { step = 1 },
                )
                else -> CityStep(
                    lang = chosenLang,
                    onDone = { cityId -> onDone(chosenLang, cityId) },
                )
            }
        }
    }
}

// ── Step 0: language ─────────────────────────────────────────────────────────

@Composable
private fun LangStep(selected: Lang, onSelect: (Lang) -> Unit, onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌟", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            "مرحباً بك في Mimo\nWelcome to Mimo",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "اختر لغة التطبيق\nChoose your language",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Lang.AR to "العربية", Lang.EN to "English").forEach { (lang, label) ->
                LangOption(label = label, selected = selected == lang, onClick = { onSelect(lang) })
            }
        }

        Spacer(Modifier.height(40.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(if (selected == Lang.AR) "التالي →" else "Next →")
        }
    }
}

@Composable
private fun LangOption(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = if (selected) CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) else CardDefaults.outlinedCardColors(),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Step 1: city ─────────────────────────────────────────────────────────────

@Composable
private fun CityStep(lang: Lang, onDone: (cityId: String?) -> Unit) {
    var selected by remember { mutableStateOf<City?>(null) }
    val isAr = lang == Lang.AR

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(
            if (isAr) "اختر مدينتك" else "Choose your city",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isAr) "تُستخدم لحساب أوقات الصلاة. يمكنك تغييرها لاحقاً."
            else "Used for prayer times. You can change it later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Cities.ALL) { city ->
                val isSelected = selected?.id == city.id
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = isSelected, onClick = { selected = city }, role = Role.RadioButton),
                    colors = if (isSelected) CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ) else CardDefaults.outlinedCardColors(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(
                            if (isAr) city.ar else city.en,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Skip — prayer times just stay disabled
            androidx.compose.material3.OutlinedButton(
                onClick = { onDone(null) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isAr) "تخطّي" else "Skip")
            }
            Button(
                onClick = { onDone(selected?.id) },
                enabled = selected != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isAr) "ابدأ 🎉" else "Start 🎉")
            }
        }
    }
}
