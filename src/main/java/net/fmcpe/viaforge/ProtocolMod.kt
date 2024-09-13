package net.fmcpe.viaforge

import net.fmcpe.viaforge.api.VFPlatform
import net.minecraft.realms.RealmsSharedConstants
import net.minecraftforge.fml.common.Mod

@Mod(modid = "ViaForge", version = "MinusBounce Beta")
class ProtocolMod : VFPlatform {
    override val gameVersion: Int
        get() = RealmsSharedConstants.NETWORK_PROTOCOL_VERSION

    companion object {
        val PLATFORM: ProtocolMod = ProtocolMod()
    }
}