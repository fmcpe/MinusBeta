package net.minusmc.minusbounce.features.module.modules.world.scaffold.tower

import net.minusmc.minusbounce.features.module.modules.world.scaffold.TowerScaffold
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.block.BlockUtils
import kotlin.math.truncate

class NCPTower: TowerScaffold("NCP") {
	override fun onPostMotion() {
		if (mc.thePlayer.posY % 1 <= 0.00153598) {
	        mc.thePlayer.setPosition(mc.thePlayer.posX, truncate(mc.thePlayer.posY), mc.thePlayer.posZ)
	        mc.thePlayer.motionY = 0.42
	    } else if (mc.thePlayer.posY % 1 < 0.1 && MovementUtils.offGroundTicks > 0)
	        mc.thePlayer.setPosition(mc.thePlayer.posX, truncate(mc.thePlayer.posY), mc.thePlayer.posZ)
	}
}