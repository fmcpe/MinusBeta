package net.fmcpe.viaforge.api

import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.version.VersionProvider
import net.raphimc.vialoader.impl.viaversion.VLLoader

class ProtocolVLLoader(private val platform: VFPlatform) : VLLoader() {
    override fun load() {
        super.load()

        val providers = Via.getManager().providers

        providers.use(VersionProvider::class.java, ProtocolVersionProvider())
    }
}