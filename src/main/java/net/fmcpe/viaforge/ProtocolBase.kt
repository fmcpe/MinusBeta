package net.fmcpe.viaforge

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.connection.UserConnectionImpl
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl
import io.netty.channel.Channel
import io.netty.channel.socket.SocketChannel
import io.netty.util.AttributeKey
import net.fmcpe.viaforge.api.*
import net.minusmc.minusbounce.MinusBounce
import net.raphimc.vialoader.ViaLoader
import net.raphimc.vialoader.impl.platform.ViaBackwardsPlatformImpl
import net.raphimc.vialoader.impl.platform.ViaLegacyPlatformImpl
import net.raphimc.vialoader.impl.platform.ViaRewindPlatformImpl
import net.raphimc.vialoader.impl.platform.ViaVersionPlatformImpl
import net.raphimc.vialoader.netty.CompressionReorderEvent
import java.util.function.Supplier

class ProtocolBase {
    @JvmField
    var targetVersion: ProtocolVersion = ProtocolVersion.v1_8
    fun inject(channel: Channel, networkManager: VFNetworkManager?) {
        if (channel is SocketChannel) {
            val user: UserConnection = UserConnectionImpl(channel, true)
            ProtocolPipelineImpl(user)

            channel.attr(LOCAL_VIA_USER).set(user)
            MinusBounce.viaUser = user
            channel.attr(VF_NETWORK_MANAGER).set(networkManager)

            channel.pipeline().addLast(ProtocolVLLegacyPipeline(user, targetVersion))
        }
    }

    fun setTargetVersionSilent(targetVersion: ProtocolVersion) {
        this.targetVersion = targetVersion
    }

    fun reorderCompression(channel: Channel) {
        channel.pipeline().fireUserEventTriggered(CompressionReorderEvent.INSTANCE)
    }

    companion object {
        val LOCAL_VIA_USER: AttributeKey<UserConnection> = AttributeKey.valueOf("local_via_user")
        val VF_NETWORK_MANAGER: AttributeKey<VFNetworkManager> = AttributeKey.valueOf("encryption_setup")
        @JvmStatic
        var manager: ProtocolBase? = null
            private set
        var versions: MutableList<ProtocolVersion> = ArrayList()

        fun init(platform: VFPlatform?) {
            if (manager != null) {
                return
            }

            manager = ProtocolBase()

            ViaLoader.init(
                ViaVersionPlatformImpl(null),
                ProtocolVLLoader(platform!!),
                ProtocolVLInjector(),
                null,
                Supplier<Any> { ViaBackwardsPlatformImpl() },
                Supplier<Any> { ViaRewindPlatformImpl() },
                Supplier<Any> { ViaLegacyPlatformImpl() },
                null
            )

            versions.addAll(ProtocolVersion.getProtocols())

            versions.removeIf { i: ProtocolVersion -> i === ProtocolVersion.unknown || i.olderThan(ProtocolVersion.v1_7_2) }

            ProtocolFixer.doFix()
        }
    }
}