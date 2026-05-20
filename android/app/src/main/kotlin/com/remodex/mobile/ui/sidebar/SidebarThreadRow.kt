package com.remodex.mobile.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remodex.mobile.R
import com.remodex.mobile.core.model.JSONValue
import com.remodex.mobile.core.model.CodexThread
import com.remodex.mobile.core.model.CodexThreadSyncState
import com.remodex.mobile.ui.theme.RemodexDropdownMenu

data class SidebarActiveChatMetadata(
    val branch: String? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    val model: String? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SidebarThreadRow(
    thread: CodexThread,
    selected: Boolean,
    isRunning: Boolean,
    activeMetadata: SidebarActiveChatMetadata? = null,
    onSelect: () -> Unit,
    onRenameRequest: (() -> Unit)? = null,
    onTogglePinnedRequest: (() -> Unit)? = null,
    onPinnedDragStart: (() -> Unit)? = null,
    onPinnedDragDelta: ((Float) -> Unit)? = null,
    onPinnedDragEnd: (() -> Unit)? = null,
    onPinnedDragCancel: (() -> Unit)? = null,
    onDeleteLocalRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    val rowShape = RoundedCornerShape(8.dp)
    var menuExpanded by remember(thread.id) { mutableStateOf(false) }
    val metadataTokens = remember(thread, selected, activeMetadata, colors) {
        if (selected) thread.activeMetadataTokens(colors, activeMetadata) else emptyList()
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = if (selected) 56.dp else 42.dp)
                .clip(rowShape)
                .background(
                    color =
                        if (selected) {
                            colors.selectedRow
                        } else {
                            Color.Transparent
                        },
                )
                .combinedClickable(
                    onClick = onSelect,
                    onLongClick =
                        if (onPinnedDragDelta == null) {
                            { onRenameRequest?.invoke() }
                        } else {
                            null
                        },
                )
                .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(6.dp).height(if (selected) 56.dp else 42.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(8.dp),
                    strokeWidth = 1.5.dp,
                    color = colors.green,
                    trackColor = colors.green.copy(alpha = 0.18f),
                )
            } else if (selected) {
                Box(
                    modifier =
                        Modifier
                            .width(3.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.green),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 7.dp, top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = thread.displayTitle,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f),
                )
            }
            val archivedNote =
                if (thread.syncState == CodexThreadSyncState.archivedLocal) {
                    stringResource(R.string.sidebar_thread_archived_hint)
                } else {
                    null
                }
            if (archivedNote != null) {
                Text(
                    text = archivedNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText,
                    maxLines = 1,
                )
            }
            if (metadataTokens.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    metadataTokens.forEachIndexed { index, token ->
                        if (index > 0) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = colors.mutedText,
                            )
                        }
                        Text(
                            text = token.text,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = token.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val rel = SidebarRelativeTimeFormatter.compactLabel(thread)
            if (rel != null) {
                Text(
                    text = rel,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = colors.mutedText,
                    maxLines = 1,
                )
            }
            if (onPinnedDragDelta != null) {
                Icon(
                    imageVector = Icons.Outlined.DragIndicator,
                    contentDescription = stringResource(R.string.sidebar_thread_reorder_pinned_cd),
                    tint = colors.mutedText,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .pointerInput(thread.id) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    var moved = false
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        val deltaY = change.position.y - change.previousPosition.y
                                        if (deltaY != 0f) {
                                            if (!moved) {
                                                moved = true
                                                onPinnedDragStart?.invoke()
                                            }
                                            change.consume()
                                            onPinnedDragDelta(deltaY)
                                        }
                                    }
                                    if (moved) {
                                        onPinnedDragEnd?.invoke()
                                    } else {
                                        onPinnedDragCancel?.invoke()
                                    }
                                }
                            },
                )
            }
            if (onRenameRequest != null || onTogglePinnedRequest != null || onDeleteLocalRequest != null) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.sidebar_thread_actions_cd),
                            tint = colors.mutedText,
                        )
                    }
                    RemodexDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (onRenameRequest != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sidebar_thread_rename)) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRenameRequest()
                                },
                            )
                        }
                        if (onTogglePinnedRequest != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (thread.isPinned) {
                                                R.string.sidebar_thread_unpin
                                            } else {
                                                R.string.sidebar_thread_pin
                                            },
                                        ),
                                    )
                                },
                                leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onTogglePinnedRequest()
                                },
                            )
                        }
                        if (onDeleteLocalRequest != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sidebar_thread_delete_local)) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteLocalRequest()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveChatRow(
    thread: CodexThread,
    isRunning: Boolean,
    activeMetadata: SidebarActiveChatMetadata? = null,
    onSelect: () -> Unit,
    onRenameRequest: (() -> Unit)?,
    onTogglePinnedRequest: (() -> Unit)?,
    onPinnedDragStart: (() -> Unit)? = null,
    onPinnedDragDelta: ((Float) -> Unit)? = null,
    onPinnedDragEnd: (() -> Unit)? = null,
    onPinnedDragCancel: (() -> Unit)? = null,
    onDeleteLocalRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SidebarThreadRow(
        thread = thread,
        selected = true,
        isRunning = isRunning,
        activeMetadata = activeMetadata,
        onSelect = onSelect,
        onRenameRequest = onRenameRequest,
        onTogglePinnedRequest = onTogglePinnedRequest,
        onPinnedDragStart = onPinnedDragStart,
        onPinnedDragDelta = onPinnedDragDelta,
        onPinnedDragEnd = onPinnedDragEnd,
        onPinnedDragCancel = onPinnedDragCancel,
        onDeleteLocalRequest = onDeleteLocalRequest,
        modifier = modifier,
    )
}

@Composable
fun ChatRow(
    thread: CodexThread,
    isRunning: Boolean,
    onSelect: () -> Unit,
    onRenameRequest: (() -> Unit)?,
    onTogglePinnedRequest: (() -> Unit)?,
    onPinnedDragStart: (() -> Unit)? = null,
    onPinnedDragDelta: ((Float) -> Unit)? = null,
    onPinnedDragEnd: (() -> Unit)? = null,
    onPinnedDragCancel: (() -> Unit)? = null,
    onDeleteLocalRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SidebarThreadRow(
        thread = thread,
        selected = false,
        isRunning = isRunning,
        activeMetadata = null,
        onSelect = onSelect,
        onRenameRequest = onRenameRequest,
        onTogglePinnedRequest = onTogglePinnedRequest,
        onPinnedDragStart = onPinnedDragStart,
        onPinnedDragDelta = onPinnedDragDelta,
        onPinnedDragEnd = onPinnedDragEnd,
        onPinnedDragCancel = onPinnedDragCancel,
        onDeleteLocalRequest = onDeleteLocalRequest,
        modifier = modifier,
    )
}

private data class SidebarMetadataToken(
    val text: String,
    val color: Color,
)

private fun CodexThread.activeMetadataTokens(
    colors: SidebarColorPalette,
    activeMetadata: SidebarActiveChatMetadata?,
): List<SidebarMetadataToken> {
    val tokens = mutableListOf<SidebarMetadataToken>()
    (
        activeMetadata?.branch?.trim()?.takeIf { it.isNotEmpty() }
            ?: firstMetadataString(
                "branch",
                "current",
                "currentBranch",
                "current_branch",
                "gitBranch",
                "git_branch",
                "headBranch",
                "head_branch",
            )
    )
        ?.let { tokens += SidebarMetadataToken(it, colors.secondaryText) }
    (
        activeMetadata?.additions
            ?: firstMetadataInt("additions", "added", "linesAdded", "lines_added", "insertions")
    )
        ?.takeIf { it > 0 }
        ?.let { tokens += SidebarMetadataToken("+$it", colors.green) }
    (
        activeMetadata?.deletions
            ?: firstMetadataInt("deletions", "deleted", "linesDeleted", "lines_deleted", "removals")
    )
        ?.takeIf { it > 0 }
        ?.let { tokens += SidebarMetadataToken("-$it", colors.red) }
    (
        activeMetadata?.model?.trim()?.takeIf { it.isNotEmpty() }
            ?: model?.trim()?.takeIf { it.isNotEmpty() }
            ?: firstMetadataString("model", "modelName", "model_name", "selectedModel", "selected_model")
    )?.let { tokens += SidebarMetadataToken(it.compactModelLabel(), colors.secondaryText) }
    return tokens
}

private fun CodexThread.firstMetadataString(vararg keys: String): String? {
    val meta = metadata ?: return null
    return keys.firstNotNullOfOrNull { key ->
        meta[key]?.stringValue?.trim()?.takeIf { it.isNotEmpty() }
    }
}

private fun CodexThread.firstMetadataInt(vararg keys: String): Int? {
    val meta = metadata ?: return null
    return keys.firstNotNullOfOrNull { key ->
        when (val value = meta[key]) {
            is JSONValue.NumLong -> value.value.toInt()
            is JSONValue.NumDouble -> value.value.toInt()
            is JSONValue.Str -> value.value.trim().toIntOrNull()
            else -> null
        }
    }
}

private fun String.compactModelLabel(): String {
    val cleaned = trim()
        .removePrefix("gpt-")
        .replace("reasoning", "", ignoreCase = true)
        .trim('-', ' ')
    return cleaned.ifEmpty { trim() }
}
