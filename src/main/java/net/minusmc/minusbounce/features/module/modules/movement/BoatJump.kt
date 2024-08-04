/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 * 
 * This code is from FDPClient.
 */
package net.minusmc.minusbounce.features.module.modules.movement

import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.UpdateEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatValue
import net.minusmc.minusbounce.value.IntegerValue
import net.minusmc.minusbounce.value.ListValue
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0CPacketInput
import net.minecraft.util.Vec3
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "BoatJump", description = "High jump when boat.", category = ModuleCategory.MOVEMENT)
class BoatJump : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Boost", "Launch", "Matrix"), "Boost")
    private val hBoostValue = FloatValue("HBoost", 2f, 0f, 6f)
    private val vBoostValue = FloatValue("VBoost", 2f, 0f, 6f)
    private val matrixTimerStartValue = FloatValue("MatrixTimerStart", 0.3f, 0.1f, 1f) { modeValue.get().equals("matrix", true) }
    private val matrixTimerAirValue = FloatValue("MatrixTimerAir", 0.5f, 0.1f, 1.5f) { modeValue.get().equals("matrix", true) }
    private val launchRadiusValue = FloatValue("LaunchRadius", 4F, 3F, 10F) { modeValue.get().equals("launch", true) }
    private val delayValue = IntegerValue("Delay", 200, 100, 500)
    private val autoHitValue = BoolValue("AutoHit", true)

    private var jumpState = 1
    private val timer = MSTimer()
    private val hitTimer = MSTimer()
    private var lastRide = false
    private var hasStopped = false

    override fun onEnable() {
        jumpState = 1
        lastRide = false
    }

    override fun onDisable() {
        hasStopped = false
        mc.timer.timerSpeed = 1f
        mc.thePlayer.speedInAir = 0.02f
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround && !mc.thePlayer.isRiding) {
            hasStopped = false
            mc.timer.timerSpeed = 1f
            mc.thePlayer.speedInAir = 0.02f
        }

        when (modeValue.get().lowercase()) {
            "matrix" -> {
                if (hasStopped) {
                    mc.timer.timerSpeed = matrixTimerAirValue.get()
                } else {
                    mc.timer.timerSpeed = 1f
                }
            }
        }

        if (mc.thePlayer.isRiding && jumpState == 1) {
            if (!lastRide) {
                timer.reset()
            }

            if (timer.hasTimePassed(delayValue.get().toLong())) {
                jumpState = 2
                when (modeValue.get().lowercase()) {
                    "matrix" -> {
                        mc.timer.timerSpeed = matrixTimerStartValue.get()
                        mc.netHandler.addToSendQueue(
                            C0CPacketInput(
                                mc.thePlayer.moveStrafing,
                                mc.thePlayer.moveForward,
                                false,
                                true
                            )
                        )
                    }
                    else -> {
                        mc.netHandler.addToSendQueue(
                            C0CPacketInput(
                                mc.thePlayer.moveStrafing,
                                mc.thePlayer.moveForward,
                                false,
                                true
                            )
                        )
                    }
                }
            }
        } else if (jumpState == 2 && !mc.thePlayer.isRiding) {
            val radiansYaw = mc.thePlayer.rotationYaw * Math.PI / 180

            when (modeValue.get().lowercase()) {
                "boost" -> {
                    mc.thePlayer.motionX = hBoostValue.get() * -sin(radiansYaw)
                    mc.thePlayer.motionZ = hBoostValue.get() * cos(radiansYaw)
                    mc.thePlayer.motionY = vBoostValue.get().toDouble()
                    jumpState = 1
                }
                "launch" -> {
                    mc.thePlayer.motionX += (hBoostValue.get() * 0.1) * -sin(radiansYaw)
                    mc.thePlayer.motionZ += (hBoostValue.get() * 0.1) * cos(radiansYaw)
                    mc.thePlayer.motionY += vBoostValue.get() * 0.1

                    var hasBoat = false
                    for (entity in mc.theWorld.loadedEntityList) {
                        if (entity is EntityBoat && mc.thePlayer.getDistanceToEntity(entity) < launchRadiusValue.get()) {
                            hasBoat = true
                            break
                        }
                    }
                    if (!hasBoat) {
                        jumpState = 1
                    }
                }
                "matrix" -> {
                    hasStopped = true
                    mc.timer.timerSpeed = matrixTimerAirValue.get()
                    mc.thePlayer.motionX = hBoostValue.get() * -sin(radiansYaw)
                    mc.thePlayer.motionZ = hBoostValue.get() * cos(radiansYaw)
                    mc.thePlayer.motionY = vBoostValue.get().toDouble()
                    jumpState = 1
                }
            }

            timer.reset()
            hitTimer.reset()
        }

        lastRide = mc.thePlayer.isRiding

        if (autoHitValue.get() && !mc.thePlayer.isRiding && hitTimer.hasTimePassed(1500)) {
            for (entity in mc.theWorld.loadedEntityList) {
                if (entity is EntityBoat && mc.thePlayer.getDistanceToEntity(entity) < 3) {
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, Vec3(0.5, 0.5, 0.5)))
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT))
                    hitTimer.reset()
                }
            }
        }
    }
}