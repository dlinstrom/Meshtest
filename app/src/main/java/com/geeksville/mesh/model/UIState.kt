package com.geeksville.mesh.model

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.RemoteException
import android.view.Menu
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.geeksville.android.Logging
import com.geeksville.mesh.*
import com.geeksville.mesh.database.MeshtasticDatabase
import com.geeksville.mesh.database.PacketRepository
import com.geeksville.mesh.database.entity.Packet
import com.geeksville.mesh.service.MeshService
import com.geeksville.mesh.ui.positionToMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/// Given a human name, strip out the first letter of the first three words and return that as the initials for
/// that user. If the original name is only one word, strip vowels from the original name and if the result is
/// 3 or more characters, use the first three characters. If not, just take the first 3 characters of the
/// original name.
fun getInitials(nameIn: String): String {
    val nchars = 3
    val minchars = 2
    val name = nameIn.trim()
    val words = name.split(Regex("\\s+")).filter { it.isNotEmpty() }

    val initials = when (words.size) {
        in 0..minchars - 1 -> {
            val nm = if (name.length >= 1)
                name.first() + name.drop(1).filterNot { c -> c.lowercase() in "aeiou" }
            else
                ""
            if (nm.length >= nchars) nm else name
        }
        else -> words.map { it.first() }.joinToString("")
    }
    return initials.take(nchars)
}

class UIViewModel(private val app: Application) : AndroidViewModel(app), Logging {

    private val repository: PacketRepository

    val allPackets: LiveData<List<Packet>>

    init {
        val packetsDao = MeshtasticDatabase.getDatabase(app).packetDao()
        repository = PacketRepository(packetsDao)
        allPackets = repository.allPackets
        debug("ViewModel created")
    }


    fun insertPacket(packet: Packet) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(packet)
    }

    fun deleteAllPacket() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }

    companion object {
        fun getPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences("ui-prefs", Context.MODE_PRIVATE)
    }

    private val context: Context get() = app.applicationContext

    var actionBarMenu: Menu? = null

    var meshService: IMeshService? = null

    val nodeDB = NodeDB(this)
    val messagesState = MessagesState(this)

    /// Are we connected to our radio device
    val isConnected =
        object :
            MutableLiveData<MeshService.ConnectionState>(MeshService.ConnectionState.DISCONNECTED) {
        }

    /// various radio settings (including the channel)
    val radioConfig = object : MutableLiveData<RadioConfigProtos.RadioConfig?>(null) {
    }

    val channels = object : MutableLiveData<ChannelSet?>(null) {
    }

    var positionBroadcastSecs: Int?
        get() {
            radioConfig.value?.preferences?.let {
                if (it.positionBroadcastSecs > 0) return it.positionBroadcastSecs
                // These default values are borrowed from the device code.
                if (it.isRouter) return 60 * 60
                return 15 * 60
            }
            return null
        }
        set(value) {
            val config = radioConfig.value
            if (value != null && config != null) {
                val builder = config.toBuilder()
                builder.preferencesBuilder.positionBroadcastSecs = value
                setRadioConfig(builder.build())
            }
        }

    var lsSleepSecs: Int?
        get() = radioConfig.value?.preferences?.lsSecs
        set(value) {
            val config = radioConfig.value
            if (value != null && config != null) {
                val builder = config.toBuilder()
                builder.preferencesBuilder.lsSecs = value
                setRadioConfig(builder.build())
            }
        }

    var locationShare: Boolean?
        get() {
            return radioConfig.value?.preferences?.locationShare == RadioConfigProtos.LocationSharing.LocEnabled
                    || radioConfig.value?.preferences?.locationShare == RadioConfigProtos.LocationSharing.LocUnset
        }
    set(value) {
            val config = radioConfig.value
            if (value != null && config != null) {
                val builder = config.toBuilder()
                if (value == true) {
                    builder.preferencesBuilder.locationShare =
                        RadioConfigProtos.LocationSharing.LocUnset
                } else {
                    builder.preferencesBuilder.locationShare =
                        RadioConfigProtos.LocationSharing.LocDisabled
                }
                setRadioConfig(builder.build())
            }
        }

    var isPowerSaving: Boolean?
        get() = radioConfig.value?.preferences?.isPowerSaving
        set(value) {
            val config = radioConfig.value
            if (value != null && config != null) {
                val builder = config.toBuilder()
                builder.preferencesBuilder.isPowerSaving = value
                setRadioConfig(builder.build())
            }
        }

    var isAlwaysPowered: Boolean?
        get() = radioConfig.value?.preferences?.isAlwaysPowered
        set(value) {
            val config = radioConfig.value
            if (value != null && config != null) {
                val builder = config.toBuilder()
                builder.preferencesBuilder.isAlwaysPowered = value
                setRadioConfig(builder.build())
            }
        }

    var region: RadioConfigProtos.RegionCode
        get() = meshService?.region?.let { RadioConfigProtos.RegionCode.forNumber(it) }
            ?: RadioConfigProtos.RegionCode.Unset
        set(value) {
            meshService?.region = value.number
        }

    /// hardware info about our local device (can be null)
    val myNodeInfo = object : MutableLiveData<MyNodeInfo?>(null) {}

    override fun onCleared() {
        super.onCleared()
        debug("ViewModel cleared")
    }

    /**
     * Return the primary channel info
     */
    val primaryChannel: Channel? get() = channels.value?.primaryChannel

    ///
    // Set the radio config (also updates our saved copy in preferences)
    private fun setRadioConfig(c: RadioConfigProtos.RadioConfig) {
        debug("Setting new radio config!")
        meshService?.radioConfig = c.toByteArray()
        radioConfig.value =
            c // Must be done after calling the service, so we will will properly throw if the service failed (and therefore not cache invalid new settings)
    }

    /// Set the radio config (also updates our saved copy in preferences)
    fun setChannels(c: ChannelSet) {
        debug("Setting new channels!")
        meshService?.channels = c.protobuf.toByteArray()
        channels.value =
            c // Must be done after calling the service, so we will will properly throw if the service failed (and therefore not cache invalid new settings)

        getPreferences(context).edit(commit = true) {
            this.putString("channel-url", c.getChannelUrl().toString())
        }
    }

    /// Kinda ugly - created in the activity but used from Compose - figure out if there is a cleaner way GIXME
    // lateinit var googleSignInClient: GoogleSignInClient

    /// our name in hte radio
    /// Note, we generate owner initials automatically for now
    /// our activity will read this from prefs or set it to the empty string
    val ownerName = object : MutableLiveData<String>("MrIDE Test") {
    }


    val bluetoothEnabled = object : MutableLiveData<Boolean>(false) {
    }

    val provideLocation = object : MutableLiveData<Boolean>(getPreferences(context).getBoolean(MyPreferences.provideLocationKey, false)) {
        override fun setValue(value: Boolean) {
            super.setValue(value)

            getPreferences(context).edit(commit = true) {
                this.putBoolean(MyPreferences.provideLocationKey, value)
            }
        }
    }

    /// If the app was launched because we received a new channel intent, the Url will be here
    var requestedChannelUrl: Uri? = null

    // clean up all this nasty owner state management FIXME
    fun setOwner(s: String? = null) {

        if (s != null) {
            ownerName.value = s

            // note: we allow an empty userstring to be written to prefs
            getPreferences(context).edit(commit = true) {
                putString("owner", s)
            }
        }

        // Note: we are careful to not set a new unique ID
        if (ownerName.value!!.isNotEmpty())
            try {
                meshService?.setOwner(
                    null,
                    ownerName.value,
                    getInitials(ownerName.value!!)
                ) // Note: we use ?. here because we might be running in the emulator
            } catch (ex: RemoteException) {
                errormsg("Can't set username on device, is device offline? ${ex.message}")
            }
    }

    /**
     * Write the persisted packet data out to a CSV file in the specified location.
     */
    fun saveMessagesCSV(file_uri: Uri) {
        viewModelScope.launch(Dispatchers.Main) {
            // Extract distances to this device from position messages and put (node,SNR,distance) in
            // the file_uri
            val myNodeNum = myNodeInfo.value?.myNodeNum ?: return@launch

            // Capture the current node value while we're still on main thread
            val nodes = nodeDB.nodes.value ?: emptyMap()

            writeToUri(file_uri) { writer ->
                // Create a map of nodes keyed by their ID
                val nodesById = nodes.values.associateBy { it.num }

                writer.appendLine("date,time,from,sender name,sender lat,sender long,rx lat,rx long,rx elevation,rx snr,distance,hop limit,payload")

                // Packets are ordered by time, we keep most recent position of
                // our device in localNodePosition.
                var localNodePosition: MeshProtos.Position? = null
                val dateFormat = SimpleDateFormat("yyyy-MM-dd,HH:mm:ss", Locale.getDefault())
                repository.allPacketsInReceiveOrder.first().forEach { packet ->
                    packet.proto?.let { proto ->
                        packet.position?.let { position ->
                            if (proto.from == myNodeNum) {
                                localNodePosition = position
                            } else {
                                val rxDateTime = dateFormat.format(packet.received_date)
                                val rxFrom = proto.from.toUInt()
                                val senderName = nodesById[proto.from]?.user?.longName ?: ""

                                // sender lat & long
                                val senderPos = packet.position
                                    ?.let { p -> Position(p) }
                                    ?.takeIf { p -> p.isValid() }
                                val senderLat = senderPos?.latitude ?: ""
                                val senderLong = senderPos?.longitude ?: ""

                                // rx lat, long, and elevation
                                val rxPos = localNodePosition
                                    ?.let { p -> Position(p) }
                                    ?.takeIf { p -> p.isValid() }
                                val rxLat = rxPos?.latitude ?: ""
                                val rxLong = rxPos?.longitude ?: ""
                                val rxAlt = rxPos?.altitude ?: ""
                                val rxSnr = "%f".format(proto.rxSnr)

                                // Calculate the distance if both positions are valid
                                val dist = if (senderPos == null || rxPos == null) {
                                    ""
                                } else {
                                    positionToMeter(
                                        localNodePosition!!,
                                        position
                                    ).roundToInt().toString()
                                }

                                val hopLimit = proto.hopLimit

                                val payload = when {
                                    proto.decoded.portnumValue != Portnums.PortNum.TEXT_MESSAGE_APP_VALUE -> "<${proto.decoded.portnum}>"
                                    proto.hasDecoded() -> "\"" + proto.decoded.payload.toStringUtf8()
                                        .replace("\"", "\\\"") + "\""
                                    proto.hasEncrypted() -> "${proto.encrypted.size()} encrypted bytes"
                                    else -> ""
                                }

                                //  date,time,from,sender name,sender lat,sender long,rx lat,rx long,rx elevation,rx snr,distance,hop limit,payload
                                writer.appendLine("$rxDateTime,$rxFrom,$senderName,$senderLat,$senderLong,$rxLat,$rxLong,$rxAlt,$rxSnr,$dist,$hopLimit,$payload")
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend inline fun writeToUri(uri: Uri, crossinline block: suspend (BufferedWriter) -> Unit) {
        withContext(Dispatchers.IO) {
            app.contentResolver.openFileDescriptor(uri, "wt")?.use { parcelFileDescriptor ->
                FileWriter(parcelFileDescriptor.fileDescriptor).use { fileWriter ->
                    BufferedWriter(fileWriter).use { writer ->
                        block.invoke(writer)
                    }
                }
            }
        }
    }

}

