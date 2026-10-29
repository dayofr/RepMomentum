package com.repmomentum.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repmomentum.ui.theme.MetricText

/** Carte de surface avec contour fin, base visuelle du design system. */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        content = content,
    )
}

/** Petit libellé en capitales monospace au-dessus d'une valeur. */
@Composable
fun MetricLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MetricText.label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        MetricLabel(label)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MetricText.counter, color = MaterialTheme.colorScheme.onSurface)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Champ numérique encadré de deux boutons d'incrément, pensé pour le pouce :
 * boutons de 56 dp, clavier numérique si on touche la valeur.
 */
@Composable
fun StepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementLabel: String,
    incrementLabel: String,
    decimal: Boolean,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton(decrementLabel, onDecrement)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                suffix = suffix?.let { { Text(it) } },
                singleLine = true,
                isError = error != null,
                textStyle = MetricText.counter.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = imeAction,
                ),
                modifier = Modifier.weight(1f),
            )
            StepButton(incrementLabel, onIncrement)
        }
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(width = 64.dp, height = 56.dp).semantics { contentDescription = text },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(text, style = MetricText.value)
    }
}
