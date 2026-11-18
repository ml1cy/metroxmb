package com.metrolist.shared.xmb

import com.metrolist.shared.deviceconnect.DeviceConnectManager
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.peerEntries
import com.metrolist.shared.xmb.adapters.peerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmbDevicesTest {
    private fun peer(
        id: String,
        state: DeviceConnectManager.PeerState = DeviceConnectManager.PeerState.CONNECTED,
        model: String = "",
    ) = DeviceConnectManager.PeerDevice(id = id, name = "Device $id", host = "10.0.0.$id", port = 1, state = state, model = model)

    @Test
    fun connectedOtherDevicesAreListedAndThePlayingOneMarked() {
        val rows =
            peerEntries(
                peers =
                    listOf(
                        peer("1"),
                        peer("2", model = "Pixel 9"),
                        peer(
                            "3",
                            DeviceConnectManager.PeerState.entries.first {
                                it !=
                                    DeviceConnectManager.PeerState.CONNECTED
                            },
                        ),
                        peer("local"),
                    ),
                localDeviceId = "local",
                targetId = "2",
                subtitle = "Device Connect",
                playingThere = "Connected",
            )
        assertEquals(listOf("1", "2"), rows.map { it.peerId() })
        assertEquals(XmbEntryKind.DEVICE, rows[0].kind)
        assertEquals("Device Connect", rows[0].subtitle)
        assertEquals("Pixel 9", rows[1].subtitle)
        assertNull(rows[0].value)
        assertEquals("Connected", rows[1].value)
    }
}
