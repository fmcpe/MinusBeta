/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.*
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.world.Scaffold
import net.minusmc.minusbounce.utils.EntityUtils.isSelected
import net.minusmc.minusbounce.utils.RotationUtils
import net.minusmc.minusbounce.utils.extensions.getDistanceToEntityBox
import net.minusmc.minusbounce.utils.movement.MovementFixType
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.utils.timer.TimeUtils
import net.minusmc.minusbounce.value.*
import org.lwjgl.input.Keyboard
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt


@Suppress("UNUSED_PARAMETER")
@ModuleInfo(name = "HvHKA", spacedName = "Kill Aura", description = "Automatically attacks targets around you.", category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
class HvHKA : Module() {
    //CPS & HurtTime
    private val cps = IntRangeValue("CPS", 5, 8, 1, 20)

    // Range & throughWalls
    private val rotationRangeValue = FloatValue("Rotation-Range", 3.7f, 1f, 8f, "m")
    private val range = FloatValue("Range", 3.7f, 1f, 8f, "m")
    private val fixServersSideMisplace = BoolValue("Fix-MisPlace", true)

    //Target / Modes
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "HurtResistance", "HurtTime", "Armor"), "Distance")
    val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "FastSwitch"), "Switch")
    private val switchDelayValue = IntegerValue("SwitchDelay", 1000, 1, 2000, "ms") {
        targetModeValue.get().equals("switch", true)
    }

    // Rotation
    private val silentRotationValue = BoolValue("SilentRotation", true)
    private val movementCorrection = ListValue("MovementFix", arrayOf("None", "Normal", "Full"), "Full")

    // Target
    private val prevTargetEntities = mutableListOf<Int>()
    private val discoveredEntities = mutableListOf<EntityLivingBase>()
    var target: EntityLivingBase? = null

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L
    private val switchTimer = MSTimer()
    var clicks = 0
    private var swing = false
    private var attackTickTimes = mutableListOf<Pair<MovingObjectPosition, Int>>()

    // Fake block status
    var blockingStatus = false
    
    private var correctedRange: Float = 0f

    override fun onDisable() {
        target = null
        switchTimer.reset()
        attackTickTimes.clear()
        clicks = 0
        prevTargetEntities.clear()
        stopBlocking(false)
        blockingStatus = false
    }

    override fun onEnable(){
        attackTickTimes.clear()
    }

    @EventTarget
    fun onWorldChange(event: WorldEvent) {
        attackTickTimes.clear()
    }

    @EventTarget
    fun onUpdate(event: PreUpdateEvent){
        RotationUtils.setRotations(
            rotation = RotationUtils.toRotation(RotationUtils.getCenter((target ?: return).entityBoundingBox)),
            speed = 360F,
            fixType = when (movementCorrection.get().lowercase()) {
                "normal" -> MovementFixType.NORMAL
                "full" -> MovementFixType.FULL
                else -> MovementFixType.NONE
            },
            silent = silentRotationValue.get()
        )
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {

        target ?: return

        if (attackTimer.hasTimePassed(attackDelay)) {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(cps.getMinValue(), cps.getMaxValue())
        }
    }

    private fun runAttack(target: Entity) {
        /* Unblock & Attack */
        if (canBlock && blockingStatus) {
            stopBlocking(false)
        }

        mc.netHandler.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))

        if (targetModeValue.get().equals("Switch", true)) {
            if (switchTimer.hasTimePassed(switchDelayValue.get().toLong())) {
                prevTargetEntities.add(target.entityId)
                switchTimer.reset()
            }
        }
    }


    @EventTarget(priority = -5)
    fun onRayTrace(e: RayTraceRangeEvent){
        if (target != null) {
            correctedRange = range.get() + if (this.fixServersSideMisplace.get()) 0.00256f else 0f
            if (this.fixServersSideMisplace.get()) {
                val n = 0.010625f
                if (mc.thePlayer.horizontalFacing === EnumFacing.NORTH || mc.thePlayer.horizontalFacing === EnumFacing.WEST) {
                    correctedRange += n * 2.0f
                }
            }
            e.range = correctedRange
            e.blockReachDistance = max(mc.playerController.blockReachDistance, correctedRange)
        }
    }
    private fun updateTarget() {
        discoveredEntities.clear()

        for (entity in mc.theWorld.loadedEntityList) {
            if (
                entity !is EntityLivingBase ||
                !isSelected(entity, true) ||
                (targetModeValue.get().equals("switch", true) && prevTargetEntities.contains(entity.entityId)) ||
                mc.thePlayer.getDistanceToEntityBox(entity) > rotationRangeValue.get()
            ) {
                continue
            }

            discoveredEntities.add(entity)
        }

        when (targetModeValue.get().lowercase()){
            "single", "switch" -> {
                when (priorityValue.get().lowercase()) {
                    "health" -> discoveredEntities.minByOrNull { it.health + it.absorptionAmount }
                    "hurtresistance" -> discoveredEntities.minByOrNull { it.hurtResistantTime }
                    "hurttime" -> discoveredEntities.minByOrNull { it.hurtTime }
                    "armor" -> discoveredEntities.minByOrNull { it.totalArmorValue }
                    else -> discoveredEntities.minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) }
                }?.let {
                    target = it
                    return
                }
            }
            else -> {
                discoveredEntities.minByOrNull {
                    val distance = mc.thePlayer.getDistanceToEntityBox(it)
                    val health = it.health + it.absorptionAmount
                    val hurtTime = it.hurtTime.toFloat()
                    val armor = it.totalArmorValue.toFloat()
                    sqrt(distance.pow(2) + health.pow(2) + hurtTime.pow(2) + armor.pow(2))
                }?.let {
                    target = it
                    return
                }
            }
        }

        target = null

        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    fun startBlocking(check: Boolean, interact: Boolean) {
        if(!blockingStatus || !check){
            if(interact){
                val target = mc.objectMouseOver.entityHit ?: return
                mc.playerController.isPlayerRightClickingOnEntity(mc.thePlayer, target, mc.objectMouseOver)
                mc.playerController.interactWithEntitySendPacket(mc.thePlayer, target)
            }

            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem() ?: return))
            blockingStatus = true
        }
    }

    fun stopBlocking(swingCheck: Boolean) {
        if(blockingStatus && (!swingCheck || !swing)){
            if(!mc.gameSettings.keyBindUseItem.isKeyDown){
                mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            } else {
                mc.gameSettings.keyBindUseItem.pressed = false
            }
            blockingStatus = false
        }
    }

    private val canBlock: Boolean
        get() = try { mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword } catch (_: Exception) { false }

    override val tag: String
        get() = targetModeValue.get()

    @EventTarget
    fun onPreMotion(event: PreMotionEvent) {
        if(target == null || mc.thePlayer.isDead || MinusBounce.moduleManager.getModule(Scaffold::class.java)?.state ?: return){
            stopBlocking(false)
            blockingStatus = false
            return
        }

    }

    @EventTarget
    fun onPostMotion(event: PostMotionEvent) {
        updateTarget()

        val target = target ?: return

        repeat(clicks) { runAttack(target); clicks-- }

        /* AutoBlock */
        if (canBlock && !blockingStatus) {
            startBlocking(false, true)
        }
    }

    @EventTarget(priority = -10)
    fun onSlowDown(event: SlowDownEvent) {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        if (canBlock && blockingStatus) {
            event.forward = 1.0F
            event.strafe = 1.0F
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if(event.eventType == EventState.SEND){
            when(packet){
                is C0APacketAnimation -> swing = true
                is C03PacketPlayer -> swing = false
            }
        }
    }
}
