package net.fmcpe.viaforge.api

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion

interface VFNetworkManager {
    fun `viaForge$getTrackedVersion`(): ProtocolVersion?

    fun `viaForge$setTrackedVersion`(version: ProtocolVersion?)
}