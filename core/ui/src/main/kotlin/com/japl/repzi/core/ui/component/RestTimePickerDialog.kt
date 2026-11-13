package com.japl.repzi.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.japl.repzi.core.ui.R
import kotlin.math.abs
import kotlinx.coroutines.launch

private val ItemHeight = 48.dp
private const val VISIBLE_ITEMS = 5

/**
 * A wheel to choose how long to rest between sets. The first entry is "no rest timer" and the rest are [options]
 * (in seconds, ascending); the entry in the middle of the wheel is the chosen one. Tapping an entry brings it to the
 * middle. [onConfirm] receives null when "no rest timer" is chosen.
 */
@Composable
fun RestTimePickerDialog(
    options: List<Int>,
    initialSeconds: Int?,
    onConfirm: (seconds: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val values = remember(options) { listOf<Int?>(null) + options }
    val initialIndex = values.indexOf(initialSeconds).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val selectedIndex by remember { derivedStateOf { listState.centeredIndex() ?: initialIndex } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rest_picker_title)) },
        text = { Wheel(values = values, listState = listState, selectedIndex = selectedIndex) },
        confirmButton = {
            TextButton(onClick = { onConfirm(values[selectedIndex]) }) {
                Text(stringResource(R.string.rest_picker_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.rest_picker_cancel)) }
        },
    )
}

@Composable
private fun Wheel(
    values: List<Int?>,
    listState: LazyListState,
    selectedIndex: Int,
) {
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxWidth().height(ItemHeight * VISIBLE_ITEMS)) {
        // The band behind the middle entry shows which one is chosen.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ItemHeight)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
            // Two entries of padding on each side let the first and the last entry reach the middle.
            contentPadding = PaddingValues(vertical = ItemHeight * (VISIBLE_ITEMS / 2)),
        ) {
            itemsIndexed(values) { index, seconds ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .semantics { selected = isSelected }
                        .clickable { scope.launch { listState.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (seconds == null) stringResource(R.string.rest_picker_none) else formatRestTime(seconds),
                        style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/** The entry whose centre is closest to the centre of the wheel, which is the one the wheel is resting on. */
private fun LazyListState.centeredIndex(): Int? {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
}
