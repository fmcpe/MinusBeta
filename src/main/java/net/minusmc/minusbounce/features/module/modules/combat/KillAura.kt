/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.block.material.Material
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.EntityUtils.isSelected
import net.minusmc.minusbounce.utils.extensions.getDistanceToEntityBox
import net.minusmc.minusbounce.utils.extensions.getNearestPointBB
import net.minusmc.minusbounce.utils.item.ItemUtils
import net.minusmc.minusbounce.utils.misc.RandomUtils
import net.minusmc.minusbounce.utils.movement.MovementFixType
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.utils.timer.TimeUtils
import net.minusmc.minusbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin


@ModuleInfo(name = "KillAura", spacedName = "Kill Aura", description = "Automatically attacks targets around you.", category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
class KillAura : Module() {

    //AutoBlock modes
    private val blockingModes = ClassUtils.resolvePackage("${this.javaClass.`package`.name}.killaura.blocking", KillAuraBlocking::class.java)
        .map { it.newInstance() as KillAuraBlocking }
        .sortedBy { it.modeName }

    private val blockingMode: KillAuraBlocking
        get() = blockingModes.find { autoBlockModeValue.get().equals(it.modeName, true) } ?: throw NullPointerException()
    
    //CPS & HurtTime
    private val cps = IntRangeValue("CPS", 5, 8, 1, 20)
    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    // Range & throughWalls
    val rangeValue = FloatValue("Range", 3.7f, 1f, 8f, "m")
    private val throughWallsValue = BoolValue("ThroughWalls", true)

    // Rotations & TurnSpeed
    private val rotations = ListValue("RotationMode", arrayOf("Vanilla", "BackTrack", "Grim", "Intave", "None"), "BackTrack")
    private val turnSpeed = FloatRangeValue("TurnSpeed", 180f, 180f, 0f, 180f, "°") {
        !rotations.get().equals("None", true)
    }

    //Target / Modes
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "HurtResistance", "HurtTime", "Armor"), "Distance")
    val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch"), "Switch")
    private val switchDelayValue = IntegerValue("SwitchDelay", 1000, 1, 2000, "ms") {
        targetModeValue.get().equals("switch", true)
    }

    // Bypass
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val intaveRandomAmount = FloatValue("Random", 4f, 0.25f, 10f) { rotations.get().equals("Intave", true) }
    private val Hitable = BoolValue("Hitable", false) { !rotations.get().equals("none", true) && !throughWallsValue.get()}

    // AutoBlock & Interact
    val autoBlockModeValue: ListValue = object : ListValue("AutoBlock", blockingModes.map { it.modeName }.toTypedArray(), "None") {
        override fun onChange(oldValue: String, newValue: String) {
            if (state) onDisable()
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state) onEnable()
        }
    }
    private val autoBlockRangeValue = FloatValue("AutoBlock-Range", 5f, 0f, 12f, "m") {
        !autoBlockModeValue.get().equals("None", true)
    }

    // Raycast & Rotation
    private val raycastValue = BoolValue("RayCast", true)
    private val silentRotationValue = BoolValue("SilentRotation", true) { !rotations.get().equals("none", true) }
    private val movementCorrection = ListValue("MovementFix", arrayOf("None", "Normal", "Full"), "Full")

    // Predict
    private val predictValue = BoolValue("Predict", true)
    private val predictSize = FloatRangeValue("PredictSize", 1f, 1f, 0.1f, 5f) {predictValue.get()}

    // Visuals
    private val espModes = ListValue("ESP", arrayOf("Jello", "Off"), "Jello")
    private val circleValue = BoolValue("Circle", true)
    private val accuracyValue = IntegerValue("Accuracy", 59, 0, 59) { circleValue.get() }
    private val red = IntegerValue("Red", 255, 0, 255) { circleValue.get() }
    private val green = IntegerValue("Green", 255, 0, 255) { circleValue.get() }
    private val blue = IntegerValue("Blue", 255, 0, 255) { circleValue.get() }
    private val alpha = IntegerValue("Alpha", 255, 0, 255) { circleValue.get() }

    // Target
    private val prevTargetEntities = mutableListOf<Int>()
    private val discoveredEntities = mutableListOf<EntityLivingBase>()
    var target: EntityLivingBase? = null
    var hitable = false
    
    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L
    private var clicks = 0
    private var attackTickTimes = mutableListOf<Pair<MovingObjectPosition, Int>>()

    // Fake block status
    var blockingStatus = false
    
    override fun onDisable() {
        target = null
        hitable = false
        attackTimer.reset()
        attackTickTimes.clear()
        clicks = 0
        prevTargetEntities.clear()
        stopBlocking()
        blockingStatus = false
        blockingMode.onDisable()
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
        blockingMode.onPreUpdate()

        updateTarget()

        /* AutoWeapon? */
        /* Find the best weapon in HotBar */
        (0..8)
            .map { Pair(it, mc.thePlayer.inventory.getStackInSlot(it)) }
            .filter { it.second != null && (it.second.item is ItemSword || it.second.item is ItemTool) }
            .maxByOrNull { (it.second.attributeModifiers["generic.attackDamage"].first()?.amount ?: 0.0) + 1.25 * ItemUtils.getEnchantment(it.second, Enchantment.sharpness) }
            ?.let{
                mc.thePlayer.inventory.currentItem = if (it.component1() != mc.thePlayer.inventory.currentItem) it.component1() else return@let
            }

        if(target == null){
            stopBlocking()
            blockingStatus = false
            return
        }

        repeat (clicks) {
            runAttack(it + 1 == clicks)
            clicks--
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (circleValue.get()) {
            GL11.glPushMatrix()
            GL11.glTranslated(
                mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * mc.timer.renderPartialTicks - mc.renderManager.renderPosX,
                mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks - mc.renderManager.renderPosY,
                mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks - mc.renderManager.renderPosZ
            )
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            GL11.glLineWidth(1F)
            GL11.glColor4f(red.get().toFloat() / 255.0F, green.get().toFloat() / 255.0F, blue.get().toFloat() / 255.0F, alpha.get().toFloat() / 255.0F)
            GL11.glRotatef(90F, 1F, 0F, 0F)
            GL11.glBegin(GL11.GL_LINE_STRIP)

            for (i in 0..360 step 60 - accuracyValue.get()) { // You can change circle accuracy  (60 - accuracy)
                GL11.glVertex2f(cos(i * Math.PI / 180.0).toFloat() * rangeValue.get(), (sin(i * Math.PI / 180.0).toFloat() * rangeValue.get()))
            }

            GL11.glEnd()

            GL11.glDisable(GL11.GL_BLEND)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)

            GL11.glPopMatrix()
        }

        target ?: return

        if (attackTimer.hasTimePassed(attackDelay) && target!!.hurtTime <= hurtTimeValue.get()) {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(cps.getMinValue(), cps.getMaxValue())
        }

        /* Draw ESP */
        when(espModes.get().lowercase()){
            "jello" -> drawCircle(target!!)
        }
    }

    /**
     * Sigma Jello Mark
     *
     */
    private fun drawCircle(it: EntityLivingBase) {
        val drawTime = (System.currentTimeMillis() % 2000).toInt()
        val drawMode=drawTime>1000
        var drawPercent=drawTime/1000.0

        if(!drawMode){
            drawPercent = 1 -drawPercent
        }else{
            drawPercent-=1
        }

        drawPercent = if (drawPercent < 0.5) { 2 * drawPercent * drawPercent } else { 1 - (-2 * drawPercent + 2).pow(2) / 2 }
        val points = mutableListOf<Vec3>()

        val bb = it.entityBoundingBox
        val radius = bb.maxX-bb.minX
        val height = bb.maxY-bb.minY

        val posX = it.lastTickPosX + (it.posX - it.lastTickPosX) * mc.timer.renderPartialTicks
        var posY = it.lastTickPosY + (it.posY - it.lastTickPosY) * mc.timer.renderPartialTicks
        if(drawMode){
            posY -= 0.5
        }else{
            posY += 0.5
        }
        val posZ = it.lastTickPosZ + (it.posZ - it.lastTickPosZ) * mc.timer.renderPartialTicks

        for(i in 0..360 step 7){
            points.add(Vec3(posX - sin(i * Math.PI / 180F) * radius,posY+height*drawPercent,posZ + cos(i * Math.PI / 180F) * radius))
        }
        points.add(points[0])

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        val baseMove= (if(drawPercent>0.5) 1-drawPercent else drawPercent)*2
        val min=(height/60)*20*(1-baseMove)*(if(drawMode) -1 else 1)

        for(i in 0..20) {
            var moveFace=(height/60F)*i*baseMove
            if(drawMode){
                moveFace=-moveFace
            }
            val firstPoint=points[0]
            GL11.glVertex3d(
                firstPoint.xCoord - mc.renderManager.viewerPosX, firstPoint.yCoord - moveFace - min - mc.renderManager.viewerPosY,
                firstPoint.zCoord - mc.renderManager.viewerPosZ
            )
            GL11.glColor4f(1F, 1F, 1F, 0.7F*(i/20F))
            for (vec3 in points) {
                GL11.glVertex3d(
                    vec3.xCoord - mc.renderManager.viewerPosX, vec3.yCoord - moveFace - min - mc.renderManager.viewerPosY,
                    vec3.zCoord - mc.renderManager.viewerPosZ
                )
            }
            GL11.glColor4f(0F,0F,0F,0F)
        }
        GL11.glEnd()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    /**
     * shouldDelayClick && runWithModifiedRaycastResult
     * @author ccbluex
     * LiquidBounce 1.8.9 (legacy)
     *
     * runAttack
     * @author fmcpe
     * Ofc me!!
     */
    private fun runAttack(isLastClicks: Boolean) {
        target ?: return

        if(hitable){
            // Attack
            attackEntity(target!!)
            prevTargetEntities.add(target!!.entityId)
        } else {
            mc.objectMouseOver?.let{ obj ->
                if (obj.typeOfHit != MovingObjectType.MISS) return@let

                if (!shouldDelayClick(obj.typeOfHit)) {
                    if (obj.typeOfHit == MovingObjectType.ENTITY) {
                        val entity = obj.entityHit

                        // Use own function instead of clickMouse() to maintain keep sprint, auto block, etc
                        if (entity is EntityLivingBase && hitable) {
                            attackEntity(entity)

                            prevTargetEntities.add(target!!.entityId)
                        }
                    } else {
                        // Imitate game click
                        mc.clickMouse()
                    }
                    attackTickTimes += (obj to runTimeTicks)
                }

                if(isLastClicks){
                    mc.sendClickBlockToController(false)
                }
            }
        }

        if (targetModeValue.get().equals("Switch", true)) {
            if (attackTimer.hasTimePassed(switchDelayValue.get().toLong())) {
                prevTargetEntities.add(target!!.entityId)
                attackTimer.reset()
            }
        }
    }

    /**
     * @author ccbluex
     * Check if raycast landed on a different object
     *
     * The game requires at least 1 tick of cooldown on raycast object type change (miss, block, entity)
     * We are doing the same thing here but allow more cool down.
     */
    private fun shouldDelayClick(type: MovingObjectType): Boolean {
        val lastAttack = attackTickTimes.lastOrNull()

        return lastAttack != null && lastAttack.first.typeOfHit != type
    }

    fun updateTarget() {
        discoveredEntities.clear()

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isSelected(entity, true) || (targetModeValue.get().equals("switch", true) && prevTargetEntities.contains(entity.entityId))) {
                continue
            }

            if (mc.thePlayer.getDistanceToEntityBox(entity) <= rangeValue.get()) {
                discoveredEntities.add(entity)
            }
        }

        when (priorityValue.get().lowercase()) {
            "health" -> discoveredEntities.sortedBy { it.health + it.absorptionAmount }
            "hurtresistance" -> discoveredEntities.sortedBy { it.hurtResistantTime }
            "hurttime" -> discoveredEntities.sortedBy { it.hurtTime }
            "armor" -> discoveredEntities.sortedBy { it.totalArmorValue }
            else -> discoveredEntities.sortedBy { mc.thePlayer.getDistanceToEntityBox(it) }
        }.forEach {
            /* Initial Hitable */
            hitable = true

            val reach = min(rangeValue.get().toDouble(), mc.thePlayer.getDistanceToEntityBox(it)) + 1

            /* Update Rotation */
            if (!rotations.get().equals("none", true)) {

                /* Do RayCast */
                val (yaw, pitch) = getTargetRotation(it)

                val rayTrace = RaycastUtils.raycastEntity(reach, yaw, pitch, object: RaycastUtils.IEntityFilter {
                    override fun canRaycast(entity: Entity?): Boolean {
                        return entity is EntityLivingBase && entity !is EntityArmorStand && isSelected(entity, true)
                    }
                })

                /* Update Target */
                target = if (raycastValue.get() && turnSpeed.getMaxValue() > 0.0F && (rayTrace ?: it) == it) (rayTrace ?: it) as EntityLivingBase else it

                RotationUtils.setRotations(
                    /* Why the f I have done this :( */
                    rotation = getTargetRotation(target ?: return@forEach),
                    keepLength = 0,
                    speed = RandomUtils.nextFloat(turnSpeed.getMinValue(), turnSpeed.getMaxValue()),
                    fixType = when (movementCorrection.get().lowercase()) {
                        "normal" -> MovementFixType.NORMAL
                        "full" -> MovementFixType.FULL
                        else -> MovementFixType.NONE
                    },
                    silent = silentRotationValue.get()
                )
            }

            /* Update Hitable && Reach bypass */
            if (!rotations.get().equals("none", true) && turnSpeed.getMaxValue() > 0.0F && Hitable.get() && mc.objectMouseOver != null) {
                hitable = (mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY && RotationUtils.isFaced(target!!, reach)) || throughWallsValue.get()
            }

            /* Return */
            return
        }

        /* If we couldn't find it */
        target = null

        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    private fun attackEntity(entity: EntityLivingBase) {
        /* Unblock & Attack */
        if (blockingStatus && !autoBlockModeValue.get().equals("none", true)) {
            blockingMode.onPreAttack()
            blockingStatus = false
        }

        /* Swing*/
        when (swingValue.get().lowercase()) {
            "normal" -> mc.thePlayer.swingItem()
            "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
        }

        when (mc.objectMouseOver.typeOfHit) {
            MovingObjectType.ENTITY -> mc.playerController.attackEntity(
                mc.thePlayer,
                if(raycastValue.get()) mc.objectMouseOver.entityHit else entity
            )

            MovingObjectType.BLOCK -> {
                if (mc.theWorld.getBlockState(mc.objectMouseOver.blockPos).block.material !== Material.air) {
                    mc.playerController.clickBlock(mc.objectMouseOver.blockPos, mc.objectMouseOver.sideHit)
                }
                if (mc.playerController.isNotCreative) {
                    mc.leftClickCounter = 10
                }
            }

            MovingObjectType.MISS -> if (mc.playerController.isNotCreative) {
                mc.leftClickCounter = 10
            }

            else -> if (mc.playerController.isNotCreative) {
                mc.leftClickCounter = 10
            }
        }

        /* AutoBlock */
        if (canBlock && mc.thePlayer.getDistanceToEntityBox(target ?: return) <= autoBlockRangeValue.get()) {
            if(!autoBlockModeValue.get().equals("none", true)){
                blockingMode.onPostAttack()
                blockingStatus = true
            }
        }
    }

    private fun getTargetRotation(entity: Entity): Rotation {
        var boundingBox = entity.entityBoundingBox

        if (predictValue.get() && !rotations.get().equals("Grim", true) && !rotations.get().equals("Intave", true)) {
            boundingBox = boundingBox.offset(
                    (entity.posX - entity.prevPosX) * RandomUtils.nextFloat(predictSize.getMinValue(), predictSize.getMaxValue()),
                    (entity.posY - entity.prevPosY) * RandomUtils.nextFloat(predictSize.getMinValue(), predictSize.getMaxValue()),
                    (entity.posZ - entity.prevPosZ) * RandomUtils.nextFloat(predictSize.getMinValue(), predictSize.getMaxValue())
            )
        }

        /* We don't override. So Vec3(0.0, 0.0, 0.0) is a good solution */
        val rotation = RotationUtils.toRotation(
            Vec3(0.0, 0.0, 0.0),
            diff = Vec3(
                entity.posX - mc.thePlayer.posX,
                entity.posY + entity.eyeHeight * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()),
                entity.posZ - mc.thePlayer.posZ
            )
        )

        val (_, vanilla) = RotationUtils.searchCenter(
            boundingBox,
            false,
            predictValue.get(),
            throughWallsValue.get(),
            rangeValue.get()
        ) ?: VecRotation(Vec3(0.0, 0.0, 0.0), rotation)

        return when (rotations.get().lowercase()) {
            /* Old BackTrack Rotation Function is getting vecRotation and DO NOTHING with it. then calculate from vec (input) */
            "backtrack" -> RotationUtils.toRotation(RotationUtils.getCenter(entity.entityBoundingBox))
            "grim" -> RotationUtils.toRotation(getNearestPointBB(mc.thePlayer.getPositionEyes(1F), boundingBox))
            "intave" -> {
                Rotation(
                    rotation.yaw + Math.random().toFloat() * intaveRandomAmount.get() - intaveRandomAmount.get() / 2,
                    rotation.pitch + Math.random().toFloat() * intaveRandomAmount.get() - intaveRandomAmount.get() / 2
                )
            }
            else -> vanilla
        }
    }

    fun startBlocking() {
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem() ?: return))
    }

    fun stopBlocking() {
        mc.playerController.onStoppedUsingItem(mc.thePlayer)
    }

    val canBlock: Boolean
        get() = mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword

    override val tag: String
        get() = targetModeValue.get()

    @EventTarget
    fun onPreMotion(event: PreMotionEvent) {
        blockingMode.onPreMotion()
    }

    @EventTarget
    fun onPostMotion(event: PostMotionEvent) {
        blockingMode.onPostMotion()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        blockingMode.onPacket(event)
    }
}
