package net.minusmc.minusbounce.features.module.modules.misc

import net.fmcpe.viaforge.packets.C1APacketSwapHand
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo

@ModuleInfo(name = "SwapHand", description = "SwapHand Left or Right", category = ModuleCategory.MISC, onlyEnable = true)
class SwapHand: Module() {
    override fun onEnable() {
        mc.netHandler.addToSendQueue(C1APacketSwapHand())
    }
}