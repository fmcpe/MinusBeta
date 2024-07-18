/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement

import net.minecraft.item.*
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.combat.KillAura
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.BlinkUtils
import net.minusmc.minusbounce.utils.ClassUtils
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatValue
import net.minusmc.minusbounce.value.ListValue

@ModuleInfo(name = "NoSlow", spacedName = "No Slow", category = ModuleCategory.MOVEMENT, description = "Prevent you from getting slowed down by items (swords, foods, etc.) and liquids.")
class NoSlow : Module() {
    private val modes = ClassUtils.resolvePackage("${this.javaClass.`package`.name}.noslows", NoSlowMode::class.java)
            .map{it.newInstance() as NoSlowMode}
            .sortedBy{it.modeName}

    val mode: NoSlowMode
        get() = modes.find { modeValue.get().equals(it.modeName, true) } ?: throw NullPointerException()

    private val modeValue: ListValue = object: ListValue("Mode", modes.map{ it.modeName }.toTypedArray(), "Vanilla") {
        override fun onChange(oldValue: String, newValue: String) {
            if (state) onDisable()
        }
        override fun onChanged(oldValue: String, newValue: String) {
            if (state) onEnable()
        }
    }
    val blockForwardMultiplier = FloatValue("BlockForwardMultiplier", 1.0F, 0.2F, 1.0F, "x")
    val blockStrafeMultiplier = FloatValue("BlockStrafeMultiplier", 1.0F, 0.2F, 1.0F, "x")
    val consumeForwardMultiplier = FloatValue("ConsumeForwardMultiplier", 1.0F, 0.2F, 1.0F, "x")
    val consumeStrafeMultiplier = FloatValue("ConsumeStrafeMultiplier", 1.0F, 0.2F, 1.0F, "x")
    val bowForwardMultiplier = FloatValue("BowForwardMultiplier", 1.0F, 0.2F, 1.0F, "x")
    val bowStrafeMultiplier = FloatValue("BowStrafeMultiplier", 1.0F, 0.2F, 1.0F, "x")
    val sneakMultiplier = FloatValue("SneakMultiplier", 1.0F, 0.3F, 1.0F, "x")

    // Soulsand
    val soulsandValue = BoolValue("Soulsand", true)
    val liquidPushValue = BoolValue("LiquidPush", true)
    private val teleportValue = BoolValue("Teleport", false)

    private var pendingFlagApplyPacket = false
    private var lastMotionX = 0.0
    private var lastMotionY = 0.0
    private var lastMotionZ = 0.0

    var lastUsingItem = false

    override fun onInitialize() {
        modes.map { mode -> mode.values.forEach { value -> value.name = "${mode.modeName}-${value.name}" } }
    }

    override fun onEnable() {
        mode.onEnable()
    }

    override fun onDisable() {
        pendingFlagApplyPacket = false
        BlinkUtils.setBlinkState(off = true)
        lastUsingItem = false
        mode.onDisable()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        mc.thePlayer ?: return
        mode.onPacket(event)

        /* Teleport (Matrix bypass) */
        val packet = event.packet
        if (teleportValue.get() && packet is S08PacketPlayerPosLook) {
            pendingFlagApplyPacket = true
            lastMotionX = mc.thePlayer.motionX
            lastMotionY = mc.thePlayer.motionY
            lastMotionZ = mc.thePlayer.motionZ
        } else if (pendingFlagApplyPacket && packet is C06PacketPlayerPosLook) {
            pendingFlagApplyPacket = false
            mc.thePlayer.motionX = lastMotionX
            mc.thePlayer.motionY = lastMotionY
            mc.thePlayer.motionZ = lastMotionZ
        }
    }

    @EventTarget
    fun onInput(event: MoveInputEvent){
        event.sneakMultiplier = sneakMultiplier.get().toDouble()
    }

    @EventTarget
    fun onPreMotion(event: PreMotionEvent) {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        if(!mc.thePlayer.isUsingItem) {
            lastUsingItem = false
            return
        }

        if (MovementUtils.isMoving){
            if (isBlocking || isEating || isBowing) {
                mode.onPreMotion(event)
            }
        }

        lastUsingItem = true
    }

    @EventTarget
    fun onPostMotion(event: PostMotionEvent) {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        if (MovementUtils.isMoving){
            if (isBlocking || isEating || isBowing) {
                mode.onPostMotion(event)
            }
        }
    }

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        mode.onSlowDown(event)
    }

    val isBlocking: Boolean
        get() = (mc.thePlayer.isBlocking || MinusBounce.moduleManager[KillAura::class.java]!!.blockingStatus) && mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword

    val isEating: Boolean
        get() = mc.thePlayer.isUsingItem && (mc.thePlayer.heldItem.item is ItemFood || mc.thePlayer.heldItem.item is ItemBucketMilk || mc.thePlayer.heldItem.item is ItemPotion && !ItemPotion.isSplash(
            mc.thePlayer.heldItem.metadata
        ))

    private val isBowing: Boolean
        get() = mc.thePlayer.isUsingItem && mc.thePlayer.heldItem.item is ItemBow

    val heldItem: ItemStack?
        get() = if(mc.thePlayer.inventory.currentItem in 0..8) {
                    mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]
        } else null

    val isSlowing: Boolean
        get() = isBlocking || isEating || isBowing

    override val tag: String
        get() = modeValue.get()

    override val values = super.values.toMutableList().also {
        modes.map {
            mode -> mode.values.forEach { value ->
                val displayableFunction = value.displayableFunction
                it.add(value.displayable { displayableFunction.invoke() && modeValue.get() == mode.modeName })
            }
        }
    }
}