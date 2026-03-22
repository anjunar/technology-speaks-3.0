package jFx2.state

import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

enum class JobState { RUNNING, COMPLETED, FAILED, CANCELED }

data class JobInfo(
    val id: String,
    val label: String,
    val owner: Any? = null,
    val startedAtMs: Long,
    val state: JobState,
    val error: Throwable? = null
)

class JobRegistry(
    private val nowMs: () -> Long = { kotlin.js.Date.now().toLong() },
    parentContext: CoroutineContext = Dispatchers.Default,
    /** 0 = sofort entfernen; >0 = noch kurz im UI sichtbar lassen */
    private val retainFinishedMs: Long = 0L,
    /** optional: harte Obergrenze, um UI-Listen klein zu halten */
    private val maxEntries: Int = 2_000
) {
    private val handler = CoroutineExceptionHandler { _, t ->
        console.error("Unhandled coroutine exception", t)
    }

    private val rootJob = SupervisorJob()
    val scope = CoroutineScope(parentContext + rootJob + handler)

    data class Entry(
        val id: String,
        val label: String,
        val owner: Any?,
        val startedAtMs: Long,
        val job: Job,
        var state: JobState,
        var error: Throwable?
    )

    val entries = ListProperty<Entry>()

    private var runningCount: Int = 0
    private var idleSinceMs: Long? = nowMs() // initial: idle
    private val idleWaiters = ArrayList<CompletableDeferred<Unit>>()

    private enum class EventType { START, END }
    private data class Event(
        val tMs: Long,
        val type: EventType,
        val id: String,
        val label: String,
        val owner: Any?,
        val state: JobState? = null
    )

    private val events = ArrayDeque<Event>()
    private val maxEvents = 200

    private fun recordEvent(e: Event) {
        events.addLast(e)
        while (events.size > maxEvents) events.removeFirst()
    }

    private fun bumpStart(id: String, label: String, owner: Any?) {
        runningCount++
        idleSinceMs = null
        recordEvent(Event(nowMs(), EventType.START, id, label, owner, state = JobState.RUNNING))
    }

    private fun bumpEnd(id: String, label: String, owner: Any?, endState: JobState) {
        if (runningCount > 0) runningCount--
        recordEvent(Event(nowMs(), EventType.END, id, label, owner, state = endState))

        if (runningCount == 0) {
            idleSinceMs = nowMs()
            val done = idleWaiters.toList()
            idleWaiters.clear()
            done.forEach { it.complete(Unit) }
        }
    }

    suspend fun awaitIdle(
        quietMs: Long = 20,
        timeoutMs: Long = 10_000
    ) {
        try {
            withTimeout(timeoutMs) {
                while (true) {
                    if (runningCount != 0) {
                        val w = CompletableDeferred<Unit>()
                        idleWaiters += w
                        w.await()
                        continue
                    }

                    if (quietMs <= 0) return@withTimeout

                    val since = idleSinceMs ?: nowMs().also { idleSinceMs = it }
                    val elapsed = nowMs() - since
                    if (elapsed >= quietMs) return@withTimeout

                    delay(quietMs - elapsed)
                }
            }
        } catch (t: TimeoutCancellationException) {
            val running = runningJobsSnapshot()
            val msg = buildTimeoutMessage(timeoutMs, quietMs, running)
            error(msg)
        }
    }

    private fun runningJobsSnapshot(): List<JobInfo> =
        entries.toList()
            .filter { it.state == JobState.RUNNING && it.job.isActive }
            .map {
                JobInfo(
                    id = it.id,
                    label = it.label,
                    owner = it.owner,
                    startedAtMs = it.startedAtMs,
                    state = it.state,
                    error = it.error
                )
            }

    private fun buildTimeoutMessage(
        timeoutMs: Long,
        quietMs: Long,
        running: List<JobInfo>
    ): String {
        val now = nowMs()

        val runningDetails = if (running.isEmpty()) {
            "RUNNING jobs: 0"
        } else {
            val s = running
                .sortedByDescending { now - it.startedAtMs }
                .joinToString("\n") { j ->
                    val age = now - j.startedAtMs
                    " - ${j.label} (id=${j.id}, owner=${j.owner}, ageMs=$age)"
                }
            "RUNNING jobs: ${running.size}\n$s"
        }

        val recent = events.takeLast(30)
        val recentDetails =
            if (recent.isEmpty()) "Recent events: (none)"
            else {
                val s = recent.joinToString("\n") { e ->
                    val dt = now - e.tMs
                    val st = e.state?.name ?: "-"
                    " - ${e.type} dtMs=$dt id=${e.id} state=$st label=${e.label} owner=${e.owner}"
                }
                "Recent events (last ${recent.size}):\n$s"
            }

        val idleSince = idleSinceMs
        val idleInfo = if (idleSince == null) {
            "idleSinceMs=null (not currently in idle window)"
        } else {
            "idleSinceMs=$idleSince (idleForMs=${now - idleSince})"
        }

        return buildString {
            append("JobRegistry.awaitIdle timed out after ${timeoutMs}ms ")
            append("(quietMs=$quietMs, runningCount=$runningCount, $idleInfo).\n")
            append(runningDetails)
            append("\n")
            append(recentDetails)
            if (running.isEmpty()) {
                append("\nHint: No RUNNING jobs at timeout. This usually means job churn (jobs start/end repeatedly) ")
                append("or work happening outside the registry. Check the recent events above and ensure *all* async work uses JobRegistry.")
            }
        }
    }

    // ------------------------------------------------------------------------

    fun snapshot(): List<JobInfo> =
        entries.toList().map {
            JobInfo(
                id = it.id,
                label = it.label,
                owner = it.owner,
                startedAtMs = it.startedAtMs,
                state = it.state,
                error = it.error
            )
        }

    fun cancelAll(cause: CancellationException = CancellationException("JobRegistry.cancelAll")) {
        rootJob.cancel(cause)
    }

    fun cancelAllFor(owner: Any, cause: CancellationException = CancellationException("cancelAllFor($owner)")) {
        entries.toList()
            .filter { it.owner == owner }
            .forEach { it.job.cancel(cause) }
    }

    private fun newId(prefix: String = "job"): String =
        "$prefix-${Random.nextInt(0, Int.MAX_VALUE)}-${nowMs()}"

    fun launch(
        label: String,
        owner: Any? = null,
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val id = newId("launch")
        bumpStart(id, label, owner)

        val job = scope.launch(context) { block() }
        registerEntry(id, label, owner, job)
        return job
    }

    fun <T> async(
        label: String,
        owner: Any? = null,
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        val id = newId("async")
        bumpStart(id, label, owner)

        val deferred = scope.async(context) { block() }
        registerEntry(id, label, owner, deferred)
        return deferred
    }

    private fun registerEntry(id: String, label: String, owner: Any?, job: Job) {
        val e = Entry(
            id = id,
            label = label,
            owner = owner,
            startedAtMs = nowMs(),
            job = job,
            state = JobState.RUNNING,
            error = null
        )
        entries.add(e)
        trimIfNeeded()

        job.invokeOnCompletion { t ->
            val endState = when {
                t == null -> JobState.COMPLETED
                t is CancellationException -> JobState.CANCELED
                else -> JobState.FAILED
            }

            scope.launch {
                e.state = endState
                if (t != null) e.error = t

                bumpEnd(id, label, owner, endState)

                if (retainFinishedMs > 0) delay(retainFinishedMs)
                window.setTimeout({ entries.remove(e) }, 3000)
            }
        }
    }

    private fun trimIfNeeded() {
        val all = entries.toList()
        val overflow = all.size - maxEntries
        if (overflow <= 0) return

        val removable = all
            .filter { it.state != JobState.RUNNING || !it.job.isActive }
            .sortedBy { it.startedAtMs }
            .take(overflow)

        removable.forEach { entries.remove(it) }
    }

    companion object {
        val instance = JobRegistry()
    }
}