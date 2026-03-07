package com.photoframe.app.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.photoframe.core.repository.SettingsRepository

/**
 * Setup wizard screen for first-time configuration.
 *
 * Shows a 3-step wizard:
 * 1. Welcome
 * 2. SMB Configuration
 * 3. Display Settings
 *
 * Phase 5: Settings & Scheduling
 * Phase 6: Polish & Bug Fixes - Integrated into first-launch flow
 *
 * Note: For MVP, this is a simplified version that guides users through
 * the initial setup but delegates actual configuration to the settings screen.
 * In production, this would be a more elaborate multi-step wizard with
 * inline configuration forms.
 *
 * @param settingsRepository Repository for checking/saving settings
 * @param onSetupComplete Callback when setup is complete
 */
@Composable
fun SetupWizardScreen(
    settingsRepository: SettingsRepository,
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (currentStep) {
            0 -> WelcomeStep(
                onNext = { currentStep = 1 }
            )
            1 -> SmbConfigStep(
                onNext = { currentStep = 2 },
                onBack = { currentStep = 0 }
            )
            2 -> DisplaySettingsStep(
                onComplete = onSetupComplete,
                onBack = { currentStep = 1 }
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Welcome to Photo Frame",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Text(
            text = "Let's set up your digital photo frame in a few simple steps.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp) // Minimum touch target
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun SmbConfigStep(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 1: SMB Configuration",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Text(
            text = "Configure your network share to access photos.\n\nYou can do this now or skip and configure later in settings.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp) // Minimum touch target
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp) // Minimum touch target
            ) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun DisplaySettingsStep(
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 2: Display Settings",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Text(
            text = "Configure how your photos are displayed.\n\nYou can customize these settings later.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp) // Minimum touch target
            ) {
                Text("Back")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp) // Minimum touch target
            ) {
                Text("Finish")
            }
        }
    }
}
