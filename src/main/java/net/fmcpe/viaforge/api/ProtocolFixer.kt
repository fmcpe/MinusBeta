package net.fmcpe.viaforge.api

import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17
import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17
import net.fmcpe.viaforge.ProtocolBase
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.utils.MinecraftInstance

object ProtocolFixer {
    private val mc = MinecraftInstance.mc

    @JvmStatic
    fun sendConditionalSwing(mop: MovingObjectPosition?) {
        if (mop != null && mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
            mc.thePlayer.swingItem()
        }
    }

    @JvmStatic
    fun sendFixedAttack(entityIn: EntityPlayer?, target: Entity?) {
        if (newerThan1_8()) {
            mc.playerController.attackEntity(entityIn, target)
            mc.thePlayer.swingItem()
        } else {
            mc.thePlayer.swingItem()
            mc.playerController.attackEntity(entityIn, target)
        }
    }

    /**
     * @author As_pw, toidicakhia
     */
    @JvmStatic
    fun doFix() {
        val transaction117 = Via.getManager().protocolManager.getProtocol(
            Protocol1_16_4To1_17::class.java
        )!!
        transaction117.registerClientbound(
            ClientboundPackets1_17.PING,
            ClientboundPackets1_16_2.WINDOW_CONFIRMATION,
            { wrapper: PacketWrapper? -> },
            true
        )
        transaction117.registerServerbound(
            ServerboundPackets1_16_2.WINDOW_CONFIRMATION,
            ServerboundPackets1_17.PONG,
            { wrapper: PacketWrapper? -> },
            true
        )
    }

    fun newerThanOrEqualsTo1_8(): Boolean {
        return ProtocolBase.manager!!.targetVersion.newerThanOrEqualTo(ProtocolVersion.v1_8) && !MinecraftInstance.mc.isIntegratedServerRunning || MinecraftInstance.mc.isIntegratedServerRunning
    }

    @JvmStatic
    fun newerThan1_8(): Boolean {
        return ProtocolBase.manager!!.targetVersion.newerThan(ProtocolVersion.v1_8) && !MinecraftInstance.mc.isIntegratedServerRunning
    }

    fun newerThanOrEqualsTo1_9(): Boolean {
        return ProtocolBase.manager!!.targetVersion.newerThanOrEqualTo(ProtocolVersion.v1_9) && !MinecraftInstance.mc.isIntegratedServerRunning
    }

    @JvmStatic
    fun newerThanOrEqualsTo1_13(): Boolean {
        return ProtocolBase.manager!!.targetVersion.newerThanOrEqualTo(ProtocolVersion.v1_13) && !MinecraftInstance.mc.isIntegratedServerRunning
    }

    fun olderThanOrEqualsTo1_13_2(): Boolean {
        return ProtocolBase.manager!!.targetVersion.olderThanOrEqualTo(ProtocolVersion.v1_13_2) && !MinecraftInstance.mc.isIntegratedServerRunning
    }

    fun newerThanOrEqualsTo1_14(): Boolean {
        return ProtocolBase.manager!!.targetVersion.newerThanOrEqualTo(ProtocolVersion.v1_14) && !MinecraftInstance.mc.isIntegratedServerRunning
    }

    @JvmStatic
    fun newerThanOrEqualsTo1_16(): Boolean {
        return ProtocolBase.manager!!.targetVersion.newerThanOrEqualTo(ProtocolVersion.v1_14) && !MinecraftInstance.mc.isIntegratedServerRunning
    }

    @JvmStatic
    fun newerThanOrEqualsTo1_17(): Boolean {
        return ProtocolBase.manager!!.targetVersion.newerThanOrEqualTo(ProtocolVersion.v1_17) && !MinecraftInstance.mc.isIntegratedServerRunning
    }
}