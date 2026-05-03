package com.wanderlog.android.data.sync

import android.content.Context
import android.os.Build
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import org.json.JSONArray
import org.json.JSONObject
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.wanderlog.android.BuildConfig
import com.wanderlog.android.domain.model.sync.TripSyncBundle
import com.wanderlog.android.domain.model.sync.TripSyncManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class NearbySyncMode {
    IDLE,
    ADVERTISING,
    DISCOVERING,
    CONNECTED
}

data class NearbySyncPeer(
    val endpointId: String,
    val endpointName: String
)

data class NearbyTripSyncState(
    val tripId: String? = null,
    val mode: NearbySyncMode = NearbySyncMode.IDLE,
    val localEndpointName: String = "",
    val discoveredPeers: List<NearbySyncPeer> = emptyList(),
    val connectedPeerName: String? = null,
    val statusMessage: String = "Idle",
    val lastSyncSummary: String? = null,
    val logLines: List<String> = emptyList(),
    val errorMessage: String? = null,
    val isBusy: Boolean = false
)

@JsonClass(generateAdapter = true)
data class NearbySyncControlMessage(
    val type: String,
    val tripId: String,
    val manifest: TripSyncManifest? = null
)

@Singleton
class NearbyTripSyncTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coordinator: TripSyncCoordinator,
    private val syncMetadataStamp: SyncMetadataStamp,
    moshi: Moshi
) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val controlAdapter = moshi.adapter(NearbySyncControlMessage::class.java)
    private val manifestAdapter = moshi.adapter(TripSyncManifest::class.java)
    private val bundleAdapter = moshi.adapter(TripSyncBundle::class.java)
    private val discoveredPeers = linkedMapOf<String, NearbySyncPeer>()
    private val knownEndpointNames = mutableMapOf<String, String>()
    private val incomingFilePayloads = mutableMapOf<Long, Pair<String, Payload>>()
    private val outgoingBundleFiles = mutableMapOf<Long, File>()

    private val _state = MutableStateFlow(NearbyTripSyncState(localEndpointName = buildLocalEndpointName()))
    val state: StateFlow<NearbyTripSyncState> = _state.asStateFlow()

    suspend fun startAdvertising(tripId: String) {
        stop()
        _state.update {
            it.copy(
                tripId = tripId,
                mode = NearbySyncMode.ADVERTISING,
                statusMessage = "Advertising trip sync",
                errorMessage = null,
                lastSyncSummary = null,
                logLines = emptyList(),
                discoveredPeers = emptyList(),
                connectedPeerName = null,
                localEndpointName = buildLocalEndpointName(),
                isBusy = false
            )
        }

        runCatching {
            connectionsClient.startAdvertising(
                buildLocalEndpointName(),
                SERVICE_ID,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
            ).await()
        }.onSuccess {
            appendLog("Advertising as ${buildLocalEndpointName()}")
        }.onFailure { error ->
            reportError("Unable to advertise sync session", error)
            stop()
        }
    }

    suspend fun startDiscovery(tripId: String?) {
        stop()
        _state.update {
            it.copy(
                tripId = tripId,
                mode = NearbySyncMode.DISCOVERING,
                statusMessage = "Discovering nearby sync hosts",
                errorMessage = null,
                lastSyncSummary = null,
                logLines = emptyList(),
                discoveredPeers = emptyList(),
                connectedPeerName = null,
                localEndpointName = buildLocalEndpointName(),
                isBusy = false
            )
        }

        runCatching {
            connectionsClient.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
            ).await()
        }.onSuccess {
            appendLog("Looking for nearby trip sync sessions")
        }.onFailure { error ->
            reportError("Unable to start discovery", error)
            stop()
        }
    }

    suspend fun requestConnection(peer: NearbySyncPeer) {
        knownEndpointNames[peer.endpointId] = peer.endpointName
        _state.update { it.copy(statusMessage = "Requesting connection to ${peer.endpointName}", isBusy = true) }
        runCatching {
            connectionsClient.requestConnection(
                buildLocalEndpointName(),
                peer.endpointId,
                connectionLifecycleCallback
            ).await()
        }.onFailure { error ->
            reportError("Unable to connect to ${peer.endpointName}", error)
            _state.update { it.copy(isBusy = false) }
        }
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        discoveredPeers.clear()
        knownEndpointNames.clear()
        incomingFilePayloads.values.forEach { (_, payload) ->
            deleteIncomingPayload(payload)
        }
        incomingFilePayloads.clear()
        outgoingBundleFiles.values.forEach(File::delete)
        outgoingBundleFiles.clear()
        _state.update {
            it.copy(
                mode = NearbySyncMode.IDLE,
                discoveredPeers = emptyList(),
                connectedPeerName = null,
                statusMessage = "Idle",
                isBusy = false
            )
        }
    }

    private suspend fun sendLocalManifest(
        endpointId: String,
        tripId: String,
        allowEmptyLocal: Boolean = false
    ) {
        val manifest = if (allowEmptyLocal) {
            coordinator.buildLocalManifestOrEmpty(tripId)
        } else {
            coordinator.buildLocalManifest(tripId) ?: return
        }
        val message = NearbySyncControlMessage(
            type = CONTROL_TYPE_MANIFEST,
            tripId = tripId,
            manifest = manifest
        )
        val bytes = controlAdapter.toJson(message).encodeToByteArray()
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes)).await()
        appendLog("Sent manifest to ${knownEndpointNames[endpointId] ?: endpointId}")
    }

    private suspend fun sendLocalBundle(endpointId: String, tripId: String) {
        val bundle = coordinator.buildLocalBundle(tripId) ?: return
        val syncDir = File(context.cacheDir, "trip-sync/outgoing").apply { mkdirs() }
        val bundleFile = File.createTempFile("$tripId-", ".json", syncDir)
        bundleFile.writeText(bundleAdapter.toJson(bundle))
        val payload = Payload.fromFile(bundleFile)
        outgoingBundleFiles[payload.id] = bundleFile
        connectionsClient.sendPayload(endpointId, payload).await()
        if (bundle.trip.metadata.deletedAt != null) {
            appendLog("Sent deleted-trip tombstone to ${knownEndpointNames[endpointId] ?: endpointId}")
        } else {
            appendLog("Sent bundle to ${knownEndpointNames[endpointId] ?: endpointId}")
        }
    }

    private suspend fun handleReceivedManifest(
        endpointId: String,
        manifest: TripSyncManifest,
        controlTripId: String
    ) {
        val remoteTripId = controlTripId.normalizedSyncTripId()
            ?: (manifest.tripId as Any?).normalizedSyncTripId()
            ?: throw IllegalStateException("Received sync manifest without a valid trip ID")

        val tripId = state.value.tripId
        if (tripId != null && tripId != remoteTripId) {
            appendLog("Ignored manifest for $remoteTripId")
            return
        }

        val normalizedManifest = if ((manifest.tripId as Any?).normalizedSyncTripId() == remoteTripId) {
            manifest
        } else {
            manifest.copy(tripId = remoteTripId)
        }

        if (tripId == null) {
            _state.update {
                it.copy(
                    tripId = remoteTripId,
                    statusMessage = "Connected to remote trip $remoteTripId"
                )
            }
        }

        val localManifest = coordinator.buildLocalManifest(remoteTripId)
        val plan = coordinator.compareWithRemoteManifestOrEmpty(normalizedManifest)
        val remoteTripRecord = normalizedManifest.records.firstOrNull { record ->
            record.entityType == com.wanderlog.android.domain.model.sync.SyncEntityType.TRIP &&
                record.id == remoteTripId
        }
        val summary = buildString {
            append("Manifest compared")
            append(": push ")
            append(plan.recordsToPush.size)
            append(", pull ")
            append(plan.recordsToPull.size)
            append(", unchanged ")
            append(plan.unchangedKeys.size)
        }
        _state.update { it.copy(lastSyncSummary = summary, statusMessage = summary) }
        appendLog(summary)

        if (remoteTripRecord?.deletedAt != null) {
            appendLog("Remote trip $remoteTripId is deleted; sync will remove the local copy if that tombstone wins")
        }

        if (plan.recordsToPush.isNotEmpty()) {
            sendLocalBundle(endpointId, remoteTripId)
        } else if (localManifest == null && plan.recordsToPull.isNotEmpty()) {
            sendLocalManifest(endpointId, remoteTripId, allowEmptyLocal = true)
            appendLog("Requested bundle for $remoteTripId")
        } else if (plan.recordsToPull.isEmpty()) {
            appendLog("Trips are already in sync")
        }
    }

    private suspend fun handleReceivedBundle(endpointId: String, payload: Payload) {
        val file = copyIncomingPayloadToCache(payload)
        if (file == null || !file.exists()) {
            appendLog("Received bundle payload with no file data")
            return
        }

        val bundle = runCatching { bundleAdapter.fromJson(file.readText()) }
            .getOrElse { error ->
                reportError("Unable to read incoming bundle", error)
                null
            }
        if (bundle == null) {
            appendLog("Unable to parse incoming sync bundle")
            file.delete()
            deleteIncomingPayload(payload)
            return
        }

        val result = coordinator.applyRemoteBundle(bundle)
        val summary = if (bundle.trip.metadata.deletedAt != null) {
            "Applied remote trip deletion"
        } else {
            "Applied ${result.appliedRecords} remote records, skipped ${result.skippedRecords}"
        }
        _state.update {
            it.copy(
                tripId = bundle.trip.id,
                lastSyncSummary = summary,
                statusMessage = summary,
                isBusy = false
            )
        }
        appendLog("Received bundle from ${knownEndpointNames[endpointId] ?: endpointId}")
        appendLog(summary)
        file.delete()
        deleteIncomingPayload(payload)
    }

    private fun copyIncomingPayloadToCache(payload: Payload): File? {
        val payloadFile = payload.asFile() ?: return null
        val incomingDir = File(context.cacheDir, "trip-sync/incoming").apply { mkdirs() }
        val targetFile = File.createTempFile("incoming-", ".json", incomingDir)

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = payloadFile.asUri() ?: return null
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                } ?: return null
            } else {
                val sourceFile = payloadFile.asJavaFile() ?: return null
                sourceFile.inputStream().use { input ->
                    FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                }
            }
            targetFile
        }.getOrElse { error ->
            targetFile.delete()
            reportError("Unable to copy incoming bundle", error)
            null
        }
    }

    private fun deleteIncomingPayload(payload: Payload) {
        val payloadFile = payload.asFile() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                payloadFile.asUri()?.let { uri ->
                    context.contentResolver.delete(uri, null, null)
                }
            } else {
                payloadFile.asJavaFile()?.delete()
            }
        }
    }

    private fun appendLog(message: String) {
        _state.update { current ->
            val nextLogs = (current.logLines + message).takeLast(MAX_LOG_LINES)
            current.copy(logLines = nextLogs)
        }
    }

    private fun reportError(message: String, throwable: Throwable) {
        val fullMessage = throwable.message?.let { "$message: $it" } ?: message
        appendLog(fullMessage)
        _state.update { it.copy(errorMessage = fullMessage, statusMessage = fullMessage, isBusy = false) }
    }

    private fun clearPeer(endpointId: String) {
        discoveredPeers.remove(endpointId)
        knownEndpointNames.remove(endpointId)
        _state.update { it.copy(discoveredPeers = discoveredPeers.values.toList()) }
    }

    private fun buildLocalEndpointName(): String {
        val model = Build.MODEL.orEmpty().ifBlank { "Android" }
        val suffix = syncMetadataStamp.currentDeviceId().takeLast(4)
        return "Wanderlog $model $suffix"
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val peer = NearbySyncPeer(endpointId = endpointId, endpointName = info.endpointName)
            discoveredPeers[endpointId] = peer
            knownEndpointNames[endpointId] = info.endpointName
            _state.update { it.copy(discoveredPeers = discoveredPeers.values.toList()) }
            appendLog("Found ${info.endpointName}")
        }

        override fun onEndpointLost(endpointId: String) {
            appendLog("Lost ${knownEndpointNames[endpointId] ?: endpointId}")
            clearPeer(endpointId)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            knownEndpointNames[endpointId] = connectionInfo.endpointName
            appendLog("Connection initiated with ${connectionInfo.endpointName}")
            scope.launch {
                runCatching {
                    connectionsClient.acceptConnection(endpointId, payloadCallback).await()
                }.onFailure { error ->
                    reportError("Unable to accept connection", error)
                }
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                val endpointName = knownEndpointNames[endpointId] ?: endpointId
                _state.update {
                    it.copy(
                        mode = NearbySyncMode.CONNECTED,
                        connectedPeerName = endpointName,
                        statusMessage = "Connected to $endpointName",
                        isBusy = false,
                        discoveredPeers = discoveredPeers.values.toList()
                    )
                }
                appendLog("Connected to $endpointName")
                scope.launch {
                    val tripId = state.value.tripId
                    if (tripId != null) {
                        runCatching { sendLocalManifest(endpointId, tripId) }
                            .onFailure { error -> reportError("Unable to send manifest", error) }
                    }
                }
            } else {
                val status = resolution.status
                appendLog("Connection failed: ${statusCodeDescription(status)}")
                _state.update { it.copy(isBusy = false, statusMessage = "Connection failed") }
            }
        }

        override fun onDisconnected(endpointId: String) {
            val endpointName = knownEndpointNames[endpointId] ?: endpointId
            appendLog("Disconnected from $endpointName")
            clearPeer(endpointId)
            _state.update {
                it.copy(
                    mode = NearbySyncMode.IDLE,
                    connectedPeerName = null,
                    statusMessage = "Disconnected",
                    isBusy = false
                )
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val bytes = payload.asBytes() ?: return
                    scope.launch {
                        runCatching {
                            val message = parseIncomingControlMessage(bytes.decodeToString())
                            if (message?.type == CONTROL_TYPE_MANIFEST && message.manifest != null) {
                                handleReceivedManifest(
                                    endpointId = endpointId,
                                    manifest = message.manifest,
                                    controlTripId = message.tripId
                                )
                            }
                        }.onFailure { error ->
                            reportError("Unable to handle control payload", error)
                        }
                    }
                }

                Payload.Type.FILE -> {
                    incomingFilePayloads[payload.id] = endpointId to payload
                    appendLog("Receiving bundle file from ${knownEndpointNames[endpointId] ?: endpointId}")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    outgoingBundleFiles.remove(update.payloadId)?.delete()
                    val pending = incomingFilePayloads.remove(update.payloadId)
                    if (pending != null) {
                        scope.launch {
                            runCatching {
                                handleReceivedBundle(endpointId = pending.first, payload = pending.second)
                            }.onFailure { error ->
                                reportError("Unable to apply incoming bundle", error)
                            }
                        }
                    }
                }

                PayloadTransferUpdate.Status.FAILURE,
                PayloadTransferUpdate.Status.CANCELED -> {
                    outgoingBundleFiles.remove(update.payloadId)?.delete()
                    incomingFilePayloads.remove(update.payloadId)?.second?.let(::deleteIncomingPayload)
                    appendLog("Payload transfer failed")
                    _state.update { it.copy(isBusy = false) }
                }
            }
        }
    }

    private fun statusCodeDescription(status: Status): String = when (status.statusCode) {
        ConnectionsStatusCodes.STATUS_OK -> "OK"
        else -> status.statusMessage ?: "status ${status.statusCode}"
    }

    private fun parseIncomingControlMessage(payload: String): NearbySyncControlMessage? =
        runCatching {
            val root = JSONObject(payload)
            val type = root.optString("type").trim().takeIf { it.isNotBlank() } ?: return null
            val controlTripId = root.opt("tripId").normalizedSyncTripId()
            val manifestJson = root.optJSONObject("manifest")
            val manifest = manifestJson?.let { json ->
                val normalizedManifestJson = json.normalizeIncomingManifestJson(controlTripId)
                manifestAdapter.fromJson(normalizedManifestJson.toString())
            }

            NearbySyncControlMessage(
                type = type,
                tripId = controlTripId
                    ?: manifest?.tripId
                    ?: "",
                manifest = manifest
            )
        }.getOrNull()

    private companion object {
        const val MAX_LOG_LINES = 20
        const val CONTROL_TYPE_MANIFEST = "manifest"
        const val SERVICE_ID = BuildConfig.APPLICATION_ID + ".trip.sync"
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    }
}

private fun Any?.normalizedSyncTripId(): String? =
    (this as? String)?.trim()?.takeIf { it.isNotBlank() }

private fun JSONObject.normalizeIncomingManifestJson(controlTripId: String?): JSONObject = JSONObject(toString()).apply {
    val normalizedTripId = opt("tripId").normalizedSyncTripId() ?: controlTripId
    if (normalizedTripId != null) {
        put("tripId", normalizedTripId)
    }
    if (!has("protocolVersion") || isNull("protocolVersion")) {
        put("protocolVersion", TripSyncManifest.CURRENT_PROTOCOL_VERSION)
    }
    if (!has("generatedAt") || isNull("generatedAt")) {
        put("generatedAt", 0L)
    }
    if (!has("records") || isNull("records")) {
        put("records", JSONArray())
    }
}
