package com.remodex.mobile.services

import com.remodex.mobile.core.error.CodexServiceError
import com.remodex.mobile.core.model.CodexThread
import com.remodex.mobile.core.model.JSONValue
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PENDING_PINNED_THREAD_IDS_TTL_MS = 10_000L

internal data class PendingPinnedThreadIds(
    val ids: List<String>,
    val expiresAtMs: Long,
)

internal suspend fun CodexService.setThreadPinnedForRepository(
    threadId: String,
    pinned: Boolean,
) = withContext(Dispatchers.IO) {
    setThreadPinnedInternal(threadId, pinned)
}

internal suspend fun CodexService.setThreadPinnedInternal(
    threadId: String,
    pinned: Boolean,
) {
    val tid = threadId.trim().takeIf { it.isNotEmpty() }
        ?: throw CodexServiceError.InvalidInput("Missing thread id")

    val nextPinnedThreadIds = resolvePinnedThreadIdsAfterToggle(_threads.value, tid, pinned)
    notePendingPinnedThreadIds(nextPinnedThreadIds)
    publishThreads(applyPinnedThreadOrderSnapshot(_threads.value, nextPinnedThreadIds))
    sendThreadPinSetRpc(tid, pinned)
    scope.launch { runCatching { refreshThreadsInternal() } }
}

internal suspend fun CodexService.setPinnedThreadOrderForRepository(threadIds: List<String>) =
    withContext(Dispatchers.IO) {
        setPinnedThreadOrderInternal(threadIds)
    }

internal suspend fun CodexService.setPinnedThreadOrderInternal(threadIds: List<String>) {
    val orderedIds = threadIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (orderedIds.isEmpty()) return

    val nextPinnedThreadIds = resolvePinnedThreadIdsAfterReorder(_threads.value, orderedIds)
    notePendingPinnedThreadIds(nextPinnedThreadIds)
    publishThreads(applyPinnedThreadOrderSnapshot(_threads.value, nextPinnedThreadIds))
    sendThreadPinsSetRpc(nextPinnedThreadIds)
    scope.launch { runCatching { refreshThreadsInternal() } }
}

private suspend fun CodexService.sendThreadPinSetRpc(
    threadId: String,
    pinned: Boolean,
) {
    sendRequestImpl(
        "thread/pin/set",
        JSONValue.Obj(
            mapOf(
                "thread_id" to JSONValue.Str(threadId),
                "pinned" to JSONValue.Bool(pinned),
            ),
        ),
    )
}

private suspend fun CodexService.sendThreadPinsSetRpc(threadIds: List<String>) {
    sendRequestImpl(
        "thread/pins/set",
        JSONValue.Obj(
            mapOf(
                "pinnedThreadIds" to JSONValue.Arr(threadIds.map { JSONValue.Str(it) }),
            ),
        ),
    )
}

internal fun CodexService.resolveIncomingPinnedThreadIds(incomingPinnedThreadIds: List<String>): List<String> {
    val pending = pendingPinnedThreadIdsOrNull() ?: return incomingPinnedThreadIds
    if (pending == incomingPinnedThreadIds) {
        pendingPinnedThreadIds = null
        return incomingPinnedThreadIds
    }
    return pending
}

internal fun CodexService.applyPendingPinnedThreadIdsIfNeeded(list: List<CodexThread>): List<CodexThread> {
    val pending = pendingPinnedThreadIdsOrNull() ?: return list
    return applyPinnedThreadOrderSnapshot(list, pending)
}

private fun CodexService.notePendingPinnedThreadIds(threadIds: List<String>) {
    pendingPinnedThreadIds =
        PendingPinnedThreadIds(
            ids = threadIds.distinct(),
            expiresAtMs = System.currentTimeMillis() + PENDING_PINNED_THREAD_IDS_TTL_MS,
        )
}

private fun CodexService.pendingPinnedThreadIdsOrNull(): List<String>? {
    val pending = pendingPinnedThreadIds ?: return null
    if (pending.expiresAtMs < System.currentTimeMillis()) {
        pendingPinnedThreadIds = null
        return null
    }
    return pending.ids
}

private fun resolvePinnedThreadIdsAfterToggle(
    list: List<CodexThread>,
    threadId: String,
    pinned: Boolean,
): List<String> {
    val existingPinnedIds =
        list
            .asSequence()
            .filter { it.id != threadId && it.isPinned }
            .sortedWith(pinnedThreadComparator())
            .map { it.id }
            .toList()
    val nextPinnedIds =
        if (pinned) {
            listOf(threadId) + existingPinnedIds
        } else {
            existingPinnedIds
        }
    return nextPinnedIds.distinct()
}

private fun resolvePinnedThreadIdsAfterReorder(
    list: List<CodexThread>,
    threadIds: List<String>,
): List<String> {
    val requested = threadIds.toSet()
    val existingPinnedTail =
        list
            .asSequence()
            .filter { it.isPinned && it.id !in requested }
            .sortedWith(pinnedThreadComparator())
            .map { it.id }
            .toList()
    return (threadIds + existingPinnedTail).distinct()
}

private fun applyPinnedThreadOrderSnapshot(
    list: List<CodexThread>,
    threadIds: List<String>,
): List<CodexThread> {
    val requested = threadIds.toSet()
    val existingPinnedTail =
        list
            .asSequence()
            .filter { it.isPinned && it.id !in requested }
            .sortedWith(pinnedThreadComparator())
            .map { it.id }
            .toList()
    val orderedIds = threadIds + existingPinnedTail
    val ranks = orderedIds.mapIndexed { index, id -> id to index }.toMap()

    return list.map { thread ->
        val rank = ranks[thread.id]
        thread.copy(
            isPinned = rank != null,
            hasPinnedState = true,
            pinnedRank = rank,
        )
    }
}

private fun pinnedThreadComparator(): Comparator<CodexThread> =
    compareBy<CodexThread> { it.pinnedRank ?: Int.MAX_VALUE }
        .thenByDescending { it.updatedAt ?: it.createdAt ?: Instant.EPOCH }
        .thenBy { it.id }
