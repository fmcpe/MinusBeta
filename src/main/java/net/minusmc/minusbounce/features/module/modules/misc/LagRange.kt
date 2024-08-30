/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.misc

import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.combat.KillAura
import net.minusmc.minusbounce.features.module.modules.player.Blink.drawBox
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.RotationUtils
import net.minusmc.minusbounce.utils.extensions.eyes
import net.minusmc.minusbounce.utils.extensions.plus
import net.minusmc.minusbounce.value.IntegerValue
import java.util.concurrent.ConcurrentLinkedQueue


@Suppress("UNUSED_PARAMETER")
@ModuleInfo(name = "LagRange", spacedName = "Lag Range", description = "Abuse latency.", category = ModuleCategory.MISC)
class LagRange : Module() {
    private var render: Vec3? = null
    private var canStart: Boolean = false
    private val outboundPackets = ConcurrentLinkedQueue<TimedPacket>()

    private val outboundDelay = IntegerValue("Delay", 250, 0, 1000)

    override val tag: String
        get() = ("- [" + outboundDelay.get() + " ms]")

    override fun onEnable() {
        if (mc.thePlayer == null || mc.isIntegratedServerRunning) {
            toggle()
            return
        }

        if (mc.thePlayer != null && !outboundPackets.isEmpty()) {
            clearPackets()
        }
    }

    override fun onDisable() {
        mc.thePlayer ?: return
        if (mc.thePlayer != null && !outboundPackets.isEmpty()) {
            outboundPackets.forEach { PacketUtils.sendPacketNoEvent(it.packet) }
            outboundPackets.clear()
        }
    }

    @EventTarget
    fun onRender(e: Render3DEvent){
        mc.thePlayer ?: return
        mc.theWorld ?: return

        if (mc.gameSettings.thirdPersonView != 0) {
            drawBox(render ?: return)
        }
    }

    @EventTarget
    fun onTick(e: TickEvent){
        while (outboundPackets.isNotEmpty()) {
            if (outboundPackets.peek().time + outboundDelay.get() <= System.currentTimeMillis()) {
                val packet = outboundPackets.poll().packet
                if(packet is C03PacketPlayer && packet.isMoving) {
                    render = Vec3(packet.x, packet.y, packet.z)
                }

                PacketUtils.sendPacketNoEvent(packet)
            } else {
                break
            }
        }

        canStart = false
    }

    @EventTarget
    fun onPacket(e: PacketEvent) {
        if (mc.thePlayer == null || mc.thePlayer.isDead || e.packet is S03PacketTimeUpdate || e.packet is S19PacketEntityStatus || e.packet is S02PacketChat || e.packet is C02PacketUseEntity) return

        val killAura = MinusBounce.moduleManager[KillAura::class.java]
        var target: EntityLivingBase? = null

        if(mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit != null && mc.objectMouseOver.entityHit is EntityLivingBase){
            target = mc.objectMouseOver.entityHit as EntityLivingBase
        }

        if(killAura?.state == true && killAura.target != null){
            target = killAura.target
        }

        if(target == null){
            return
        }

        val positionEyes = mc.thePlayer.eyes
        val positionEyesServer = serverPosition + Vec3(0.0, mc.thePlayer.getEyeHeight().toDouble(), 0.0)
        val bestHitVec = RotationUtils.getBestHitVec(target)
        canStart = e.eventType == EventState.SEND && positionEyes.distanceTo(bestHitVec) > 2.9 && positionEyes.distanceTo(bestHitVec) < positionEyesServer.distanceTo(bestHitVec)

        if(canStart){
            outboundPackets.add(TimedPacket(e.packet))
            e.isCancelled = true
            e.stopRunEvent = true
        }
    }

    private fun clearPackets() {
        outboundPackets.clear()
    }
}

data class TimedPacket(val packet: Packet<*>, val time: Long = System.currentTimeMillis())