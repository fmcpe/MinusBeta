package net.fmcpe.viaforge.api

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.protocols.base.BaseVersionProvider
import net.fmcpe.viaforge.ProtocolBase
import net.minusmc.minusbounce.utils.MinecraftInstance
import java.util.*

class ProtocolVersionProvider : BaseVersionProvider() {
    @Throws(Exception::class)
    override fun getClosestServerProtocol(connection: UserConnection): ProtocolVersion {
        if (connection.isClientSide && !MinecraftInstance.mc.isIntegratedServerRunning) {
            return Objects.requireNonNull(connection.channel)!!.attr(ProtocolBase.VF_NETWORK_MANAGER).get()
                .`viaForge$getTrackedVersion`()!!
        }
        return super.getClosestServerProtocol(connection)
    }
}