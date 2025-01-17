/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.processor

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.datadog.android.sessionreplay.RecordWriter
import com.datadog.android.sessionreplay.model.MobileSegment
import com.datadog.android.sessionreplay.recorder.Node
import com.datadog.android.sessionreplay.recorder.OrientationChanged
import com.datadog.android.sessionreplay.utils.RumContextProvider
import com.datadog.android.sessionreplay.utils.SessionReplayRumContext
import com.datadog.android.sessionreplay.utils.TimeProvider
import java.lang.NullPointerException
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
internal class RecordedDataProcessor(
    private val rumContextProvider: RumContextProvider,
    private val timeProvider: TimeProvider,
    private val executorService: ExecutorService,
    private val writer: RecordWriter,
    private val mutationResolver: MutationResolver = MutationResolver(),
    private val nodeFlattener: NodeFlattener = NodeFlattener()
) : Processor {

    internal var prevRumContext: SessionReplayRumContext = SessionReplayRumContext()
    private var prevSnapshot: List<MobileSegment.Wireframe> = emptyList()
    private var lastSnapshotTimestamp = 0L

    @MainThread
    override fun processScreenSnapshots(
        nodes: List<Node>,
        orientationChanged: OrientationChanged?
    ) {
        buildRunnable { timestamp, newContext, currentContext ->
            Runnable {
                @Suppress("ThreadSafety") // this runs inside an executor
                handleSnapshots(
                    newContext,
                    currentContext,
                    timestamp,
                    nodes,
                    orientationChanged
                )
            }
        }?.let { executeRunnable(it) }
    }

    @MainThread
    override fun processTouchEventsRecords(touchEventsRecords: List<MobileSegment.MobileRecord>) {
        buildRunnable { _, newContext, _ ->
            Runnable {
                @Suppress("ThreadSafety") // this runs inside an executor
                handleTouchRecords(newContext, touchEventsRecords)
            }
        }?.let { executeRunnable(it) }
    }

    // region Internal

    @WorkerThread
    private fun handleTouchRecords(
        rumContext: SessionReplayRumContext,
        touchData: List<MobileSegment.MobileRecord>
    ) {
        val enrichedRecord = bundleRecordInEnrichedRecord(rumContext, touchData)
        writer.write(enrichedRecord)
    }

    @WorkerThread
    private fun handleSnapshots(
        newRumContext: SessionReplayRumContext,
        prevRumContext: SessionReplayRumContext,
        timestamp: Long,
        snapshots: List<Node>,
        orientationChanged: OrientationChanged?
    ) {
        val wireframes = snapshots.flatMap { nodeFlattener.flattenNode(it) }

        if (wireframes.isEmpty()) {
            // TODO: RUMM-2397 Add the proper logs here once the sdkLogger will be added
            return
        }

        val records: MutableList<MobileSegment.MobileRecord> = LinkedList()
        val isNewView = isNewView(prevRumContext, newRumContext)
        val isTimeForFullSnapshot = isTimeForFullSnapshot()
        val fullSnapshotRequired = isNewView ||
            isTimeForFullSnapshot ||
            (orientationChanged != null)

        if (isNewView) {
            handleViewEndRecord(prevRumContext, timestamp)
            val rootWireframeBounds = wireframes[0].bounds()
            val metaRecord = MobileSegment.MobileRecord.MetaRecord(
                timestamp,
                MobileSegment.Data1(rootWireframeBounds.width, rootWireframeBounds.height)
            )
            val focusRecord = MobileSegment.MobileRecord.FocusRecord(
                timestamp,
                MobileSegment.Data2(true)
            )
            records.add(metaRecord)
            records.add(focusRecord)
        }

        if (orientationChanged != null) {
            val viewPortResizeData = MobileSegment.MobileIncrementalData.ViewportResizeData(
                orientationChanged.width.toLong(),
                orientationChanged.height.toLong()
            )
            val viewportRecord = MobileSegment.MobileRecord.MobileIncrementalSnapshotRecord(
                timestamp,
                data = viewPortResizeData
            )
            records.add(viewportRecord)
        }

        if (fullSnapshotRequired) {
            records.add(
                MobileSegment.MobileRecord.MobileFullSnapshotRecord(
                    timestamp,
                    MobileSegment.Data(wireframes)
                )
            )
        } else {
            mutationResolver.resolveMutations(prevSnapshot, wireframes)?.let {
                records.add(
                    MobileSegment.MobileRecord.MobileIncrementalSnapshotRecord(
                        timestamp,
                        it
                    )
                )
            }
        }
        prevSnapshot = wireframes
        if (records.isNotEmpty()) {
            writer.write(bundleRecordInEnrichedRecord(newRumContext, records))
        }
    }

    private fun isTimeForFullSnapshot(): Boolean {
        return if (System.nanoTime() - lastSnapshotTimestamp >= FULL_SNAPSHOT_INTERVAL_IN_NS) {
            lastSnapshotTimestamp = System.nanoTime()
            true
        } else {
            false
        }
    }

    private fun handleViewEndRecord(prevRumContext: SessionReplayRumContext, timestamp: Long) {
        if (prevRumContext.isValid()) {
            // send first the ViewEndRecord for the previous RUM context (View)
            val viewEndRecord = MobileSegment.MobileRecord.ViewEndRecord(timestamp)
            writer.write(bundleRecordInEnrichedRecord(prevRumContext, listOf(viewEndRecord)))
        }
    }

    private fun executeRunnable(runnable: Runnable) {
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        try {
            executorService.submit(runnable)
        } catch (e: RejectedExecutionException) {
            // TODO: RUMM-2397 Add the proper logs here once the sdkLogger will be added
        } catch (e: NullPointerException) {
            // TODO: RUMM-2397 Add the proper logs here once the sdkLogger will be added
            // the task will never be null so normally this exception should not be triggered.
            // In any case we will add a log here later.
        }
    }

    private fun buildRunnable(
        runnableFactory: (
            timestamp: Long,
            newContext: SessionReplayRumContext,
            prevRumContext: SessionReplayRumContext
        ) -> Runnable
    ): Runnable? {
        // we will make sure we get the timestamp on the UI thread to avoid time skewing
        val timestamp = timeProvider.getDeviceTimestamp()

        // TODO: RUMM-2426 Fetch the RumContext from the core SDKContext when available
        val newRumContext = rumContextProvider.getRumContext()

        if (newRumContext.isNotValid()) {
            // TODO: RUMM-2397 Add the proper logs here once the sdkLogger will be added
            return null
        }
        val runnable = runnableFactory(timestamp, newRumContext.copy(), prevRumContext.copy())

        prevRumContext = newRumContext

        return runnable
    }

    private fun bundleRecordInEnrichedRecord(
        rumContext: SessionReplayRumContext,
        records: List<MobileSegment.MobileRecord>
    ):
        EnrichedRecord {
        return EnrichedRecord(
            rumContext.applicationId,
            rumContext.sessionId,
            rumContext.viewId,
            records
        )
    }

    private fun isNewView(
        newContext: SessionReplayRumContext,
        currentContext: SessionReplayRumContext
    ): Boolean {
        return newContext.applicationId != currentContext.applicationId ||
            newContext.sessionId != currentContext.sessionId ||
            newContext.viewId != currentContext.viewId
    }

    private fun MobileSegment.Wireframe.bounds(): Bounds {
        return when (this) {
            is MobileSegment.Wireframe.ShapeWireframe -> bounds()
            is MobileSegment.Wireframe.TextWireframe -> bounds()
        }
    }

    private fun MobileSegment.Wireframe.ShapeWireframe.bounds(): Bounds {
        return Bounds(x, y, width, height)
    }

    private fun MobileSegment.Wireframe.TextWireframe.bounds(): Bounds {
        return Bounds(x, y, width, height)
    }

    private data class Bounds(val x: Long, val y: Long, val width: Long, val height: Long)

    // endregion

    companion object {
        internal val FULL_SNAPSHOT_INTERVAL_IN_NS = TimeUnit.MILLISECONDS.toNanos(3000)
    }
}
