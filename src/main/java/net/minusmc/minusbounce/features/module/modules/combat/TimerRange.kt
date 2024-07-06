package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.MathHelper
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.world.Scaffold
import net.minusmc.minusbounce.utils.MovementUtils.speed
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.RotationUtils
import net.minusmc.minusbounce.utils.extensions.getDistanceToEntityBox
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatRangeValue
import net.minusmc.minusbounce.value.FloatValue
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt


@ModuleInfo("TimerRange", "Timer Range", "Allows you to teleport to player", category = ModuleCategory.COMBAT)
class TimerRange: Module(){
    private val range = FloatRangeValue("Range", 6.0F, 6.0F, 3.0F, 8.0F)
    private val maxTimer = FloatValue("Timer", 10.0f, 1.0f, 10.0f)
    private val slowTimer = FloatValue("SlowTimer", 0.0f, 0.0f, 1.0f)
    private val chargeMultiplier = FloatValue("ChargeMultiplier", 1.0f, 0.0f, 1.0f)
    private val delay = FloatValue("Delay", 200.0f, 0.0f, 3000.0f)
    private val instantTimer = BoolValue("InstantTimer", true)
    private val notInCombo = BoolValue("NotInCombo", true)
    private val onlyForward = BoolValue("OnlyForward", true)
    private val preLoad = BoolValue("PreLoad", false)
    private val blink = BoolValue("Blink", false)
    private val onlyOnGround = BoolValue("OnlyOnGround", false)
    private val noFluid = BoolValue("NoFluid", true)

    // Settings
    private var outPackets = LinkedList<Packet<*>>()
    private var balance = 0.0
    private var lastBalance = 0.0
    private var smartMaxBalance = 0.0
    private var fast = false
    private var target: EntityLivingBase? = null
    private val delayTimer = MSTimer()
    private val clickTimer = MSTimer()

    override fun onDisable() {
        this.outPackets.clear()
    }

    override val tag: String
        get() = range.getMaxValue().toString() + ""

    @EventTarget
    fun onPacket(e: PacketEvent) {
        val packet = e.packet
        if (packet is C03PacketPlayer && e.eventType == EventState.SEND) {
            if (mc.timer.timerSpeed > 1.0f && blink.get()) {
                e.cancelEvent()
                outPackets.add(e.packet)
            }
        }
    }

    @EventTarget
    fun onWorld(e: WorldEvent) {
        this.balance = 0.0
        this.lastBalance = 0.0
    }

    @EventTarget
    fun onTick(e: TickEvent?) {
        if (preLoad.get()) {
            if (mc.timer.timerSpeed != 1.0f) {
                this.balance += chargeMultiplier.get().toDouble()
            } else {
                ++this.balance
            }
        } else if (this.fast) {
            this.balance += chargeMultiplier.get().toDouble()
        } else {
            ++this.balance
        }

        if (this.shouldStop() && this.fast) {
            this.release()
            mc.timer.timerSpeed = 1.0f
            this.fast = false
        }
    }

    @EventTarget
    fun onTick(e: TimeDelay) {
        --this.balance
    }

    @EventTarget
    fun onLoop(e: GameLoop) {
        this.target = MinusBounce.moduleManager.getModule(KillAura::class.java)?.target ?: return
        if (mc.thePlayer != null && mc.theWorld != null && MinusBounce.moduleManager.getModule(KillAura::class.java)?.state == true && MinusBounce.moduleManager.getModule(Scaffold::class.java)?.state == false && mc.thePlayer.ticksExisted >= 10
        ) {
            if (this.target != null && this.outOfRange()) {
                this.target = null
            }

            if (this.fast) {
                run label205@{
                    run label204@{
                        if (this.preLoad.get()) {
                            if (this.balance < this.lastBalance) {
                                return@label204
                            }
                        } else if (this.balance < this.smartMaxBalance + this.lastBalance) {
                            return@label204
                        }

                        mc.timer.timerSpeed = 1.0F
                        this.fast = false
                        if (this.preLoad.get()) {
                            this.delayTimer.reset()
                        }
                        return@label205
                    }

                    if (this.target != null) {
                        var rayTracedEntity: Entity?
                        if (!this.isTargetCloseOrVisible()) {
                            if (this.isHurtTime()) {
                                if (this.instantTimer.get()) {
                                    var shouldStop = false
                                    while (!shouldStop) {
                                        run label220@{
                                            if (!this.isTargetCloseOrVisible() && this.isHurtTime() && (this.preLoad.get() || !this.shouldStop())) {
                                                if (this.preLoad.get()) {
                                                    if (!(this.balance >= this.lastBalance)) {
                                                        return@label220
                                                    }
                                                } else if (!(this.balance >= this.smartMaxBalance + this.lastBalance)) {
                                                    return@label220
                                                }
                                            }
                                            shouldStop = true
                                            this.delayTimer.reset()
                                            this.release()
                                            mc.timer.timerSpeed = 1.0F
                                            this.fast = false
                                            if (this.preLoad.get()) {
                                                this.delayTimer.reset()
                                            }
                                            if (this.clickTimer.hasTimeElapsed(350.0, true)) {
                                                MinusBounce.eventManager.callEvent(AttackEvent(this.target))
                                                rayTracedEntity = RotationUtils.rayTrace(3.0, RotationUtils.targetRotation ?: return)
                                                if (rayTracedEntity != null) {
                                                    mc.playerController.attackEntity(mc.thePlayer, rayTracedEntity)
                                                }
                                            }
                                        }
                                        if (!shouldStop) {
                                            mc.runTick()
                                            this.balance += this.chargeMultiplier.get()
                                        }
                                    }
                                } else {
                                    mc.timer.timerSpeed = this.maxTimer.get()
                                    if (this.shouldStop() && this.fast) {
                                        this.release()
                                        mc.timer.timerSpeed = 1.0F
                                        this.fast = false
                                        if (this.preLoad.get()) {
                                            this.delayTimer.reset()
                                        }
                                        if (this.clickTimer.hasTimeElapsed(350.0, true)) {
                                            MinusBounce.eventManager.callEvent(AttackEvent(this.target))
                                            rayTracedEntity = RotationUtils.rayTrace(3.0, RotationUtils.targetRotation ?: return)
                                            if (rayTracedEntity != null) {
                                                mc.playerController.attackEntity(mc.thePlayer, rayTracedEntity)
                                            }
                                        }
                                    }
                                }
                            } else if (!this.preLoad.get()) {
                                mc.timer.timerSpeed = 1.0F
                                this.fast = false
                                if (this.preLoad.get()) {
                                    this.delayTimer.reset()
                                }
                            }
                        } else {
                            this.release()
                            mc.timer.timerSpeed = 1.0F
                            this.fast = false
                            if (this.preLoad.get()) {
                                this.delayTimer.reset()
                            }
                            if (this.clickTimer.hasTimeElapsed(350.0, true)) {
                                MinusBounce.eventManager.callEvent(AttackEvent(this.target))
                                rayTracedEntity = RotationUtils.rayTrace(3.0, RotationUtils.targetRotation ?: return)
                                if (rayTracedEntity != null) {
                                    mc.playerController.attackEntity(mc.thePlayer, rayTracedEntity)
                                }
                            }
                        }
                    } else {
                        mc.timer.timerSpeed = 1.0F
                        this.fast = false
                        if (this.preLoad.get()) {
                            this.delayTimer.reset()
                        }
                    }
                }
            }

            if (!this.fast) {
                if (preLoad.get()) {
                    if (!delayTimer.hasTimeElapsed(delay.get().toDouble(), false)) {
                        return
                    }

                    if (this.target != null) {
                        if (!this.shouldStop() && mc.timer.timerSpeed == 1.0f) {
                            this.setSmartBalance()
                        }

                        if (!this.isTargetCloseOrVisible() && this.isHurtTime()) {
                            if (this.balance > -this.smartMaxBalance + this.lastBalance) {
                                if (this.shouldStop()) {
                                    if (mc.timer.timerSpeed != slowTimer.get()) {
                                        this.lastBalance = this.balance
                                    }

                                    mc.timer.timerSpeed = 1.0f
                                    return
                                }

                                mc.timer.timerSpeed = slowTimer.get()
                            } else {
                                this.fast = true
                                mc.timer.timerSpeed = 1.0f
                            }
                        } else {
                            if (mc.timer.timerSpeed != slowTimer.get()) {
                                this.lastBalance = this.balance
                            }

                            mc.timer.timerSpeed = 1.0f
                        }
                    } else {
                        if (mc.timer.timerSpeed != slowTimer.get()) {
                            this.lastBalance = this.balance
                        }

                        mc.timer.timerSpeed = 1.0f
                    }

                    this.release()
                } else {
                    if (this.balance > this.lastBalance) {
                        mc.timer.timerSpeed = slowTimer.get()
                    } else {
                        if (mc.timer.timerSpeed == slowTimer.get()) {
                            mc.timer.timerSpeed = 1.0f
                        }

                        if (!delayTimer.hasTimeElapsed(delay.get().toDouble(), false)) {
                            return
                        }

                        if ((this.target != null) && !this.isTargetCloseOrVisible() && this.isHurtTime()) {
                            this.fast = true
                            delayTimer.reset()
                            this.lastBalance = this.balance
                        }
                    }

                    this.release()
                }

                if (this.fast && !preLoad.get()) {
                    this.setSmartBalance()
                }
            }

            if (mc.thePlayer.ticksExisted <= 20) {
                mc.timer.timerSpeed = 1.0f
            }
        } else {
            if (mc.thePlayer != null && mc.thePlayer.ticksExisted < 20) {
                mc.timer.timerSpeed = 1.0f
            }

            if (mc.timer.timerSpeed.toDouble() == slowTimer.get().toDouble()) {
                mc.timer.timerSpeed = 1.0f
            }

            this.target = null
        }
    }

    private fun release() {
        while (true) {
            try {
                if (!outPackets.isEmpty()) {
                    PacketUtils.sendPacketNoEvent(outPackets.poll() as Packet<*>)
                    continue
                }
            } catch (_: Exception) { }

            return
        }
    }

    private fun setSmartBalance() {
        val distance = mc.thePlayer.getDistanceToEntity(this.target).toDouble() - abs(
            this.distanceAdjust()
        )
        if (this.shouldStop()) {
            this.smartMaxBalance = 0.0
        } else {
            var playerBPS =
                sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ)
            val targetMotionX = abs(target!!.lastTickPosX - target!!.posX)
            val targetMotionZ = abs(target!!.lastTickPosZ - target!!.posZ)
            var targetBPS = sqrt(targetMotionX * targetMotionX + targetMotionZ * targetMotionZ)
            playerBPS = max(0.15, playerBPS)
            targetBPS = max(if (preLoad.get()) 0.15 else 0.0, targetBPS)
            val finalDistance = distance - 3.0
            if (preLoad.get()) {
                this.smartMaxBalance = finalDistance / (playerBPS + targetBPS * 3.0)
            } else {
                this.smartMaxBalance = finalDistance / (playerBPS * 2.0)
            }
        }
    }

    private fun shouldStop(): Boolean {
        val backTrack = MinusBounce.moduleManager.getModule(BackTrack::class.java) ?: return false
        var stop = false
        if (this.target == null) {
            return true
        } else {
            val predictX = mc.thePlayer.posX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * 2.0
            val predictZ = mc.thePlayer.posZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * 2.0
            val f = (predictX - target!!.posX).toFloat()
            val f1 = (mc.thePlayer.posY - target!!.posY).toFloat()
            val f2 = (predictZ - target!!.posZ).toFloat()
            val predictedDistance = MathHelper.sqrt_float(f * f + f1 * f1 + f2 * f2).toDouble()
            if (onlyOnGround.get() && !mc.thePlayer.onGround) {
                stop = true
            }

            if (mc.thePlayer.getDistanceToEntityBox(target!!) < range.getMinValue()) {
                if (preLoad.get()) {
                    if (!this.fast) {
                        stop = true
                    }
                } else if (!this.fast && mc.timer.timerSpeed.toDouble() != slowTimer.get().toDouble()) {
                    stop = true
                }
            }

            if (this.isTargetCloseOrVisible()) {
                stop = true
            }

            if (!this.isHurtTime()) {
                stop = true
            }

            if (this.outOfRange()) {
                stop = true
            }

            if ((speed <= 0.08 || !mc.gameSettings.keyBindForward.pressed || predictedDistance > mc.thePlayer.getDistanceToEntity(
                    target
                ).toDouble() + 0.08) && onlyForward.get()
            ) {
                stop = true
            }

            if (this.outOfRange()) {
                stop = true
            }

            if (noFluid.get() && (mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.isInWeb)) {
                stop = true
            }

            if (mc.thePlayer.getDistance(
                    target!!.lastTickPosX,
                    target!!.lastTickPosY, target!!.lastTickPosZ
                ) < mc.thePlayer.getDistance(
                    target!!.posX, target!!.posY, target!!.posZ
                )
            ) {
                stop = notInCombo.get()
            }

            if (backTrack.state && !backTrack.delayedPackets.isEmpty() && mc.thePlayer.getDistance(
                    target!!.posX,
                    target!!.posY,
                    target!!.posZ
                ) < mc.thePlayer.getDistance(
                    target!!.serverPosX.toDouble(),
                    target!!.serverPosY.toDouble(),
                    target!!.serverPosZ.toDouble()
                )
            ) {
                stop = notInCombo.get()
            }

            return if (this.fast) stop else (if (preLoad.get()) stop else false)
        }
    }

    private fun outOfRange(): Boolean {
        return mc.thePlayer.getDistanceToEntity(this.target).toDouble() > this.getMaxDistance() + this.distanceAdjust()
    }

    private fun isTargetCloseOrVisible(): Boolean {
        return mc.objectMouseOver.entityHit != null || mc.thePlayer.getDistanceToEntityBox(this.target!!) <= 3.0
    }

    private fun isHurtTime(): Boolean {
        return mc.thePlayer.hurtTime <= (if (!preLoad.get()) 10 else 10)
    }

    private fun distanceAdjust(): Double {
        return if (mc.thePlayer.getDistance(
                target!!.lastTickPosX,
                target!!.lastTickPosY, target!!.lastTickPosZ
            ) < mc.thePlayer.getDistance(
                target!!.posX, target!!.posY, target!!.posZ
            ) - 0.05
        ) {
            -0.5
        } else {
            if (mc.thePlayer.getDistance(
                    target!!.lastTickPosX,
                    target!!.lastTickPosY, target!!.lastTickPosZ
                ) > mc.thePlayer.getDistance(
                    target!!.posX, target!!.posY, target!!.posZ
                ) + 0.1
            ) 0.3 else 0.0
        }
    }

    private fun getMaxDistance(): Double {
        return range.getMaxValue() + 1.0
    }
}