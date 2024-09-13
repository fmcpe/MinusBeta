package net.fmcpe.viaforge.api

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion

interface ExtendedServerData {
    fun `viaForge$getVersion`(): ProtocolVersion?

    fun `viaForge$setVersion`(version: ProtocolVersion?)
}