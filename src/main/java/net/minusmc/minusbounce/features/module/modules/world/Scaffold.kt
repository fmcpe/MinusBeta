package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.*
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.injection.access.StaticStorage
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP
import net.minusmc.minusbounce.ui.font.Fonts
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.InventoryUtils.BLOCK_BLACKLIST
import net.minusmc.minusbounce.utils.block.BlockUtils
import net.minusmc.minusbounce.utils.block.BlockUtils.rayTrace
import net.minusmc.minusbounce.utils.block.PlaceInfo
import net.minusmc.minusbounce.utils.extensions.eyes
import net.minusmc.minusbounce.utils.extensions.iterator
import net.minusmc.minusbounce.utils.extensions.plus
import net.minusmc.minusbounce.utils.misc.RandomUtils
import net.minusmc.minusbounce.utils.movement.MovementFixType
import net.minusmc.minusbounce.utils.render.RenderUtils
import net.minusmc.minusbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt


@ModuleInfo("Scaffold", "Scaffold", "Use huge balls to rolling on mid-air", ModuleCategory.WORLD)
class Scaffold: Module(){

    private val modes = ListValue("Mode", arrayOf("Normal", "Snap", "Telly", "None", "GodBridge", "Legit"), "Normal")
    private val clicks = ListValue("ClickMode", arrayOf("Legit", "RayTraced", "Normal"), "RayTraced")
    private val reset = BoolValue("RotationActivateReset", false) { modes.get() == "Telly" }
    private val ticks = IntegerValue("Ticks", 0, 0, 10) { modes.get() == "Telly" }
    private val delayValue = IntRangeValue("Delay", 0, 0, 0, 10)
    private val sprint = ListValue("Sprint", arrayOf("Normal", "Legit", "VulcanToggle", "Omni", "Off"), "Normal")

    private val speed = FloatRangeValue("Speed", 90f, 90f, 0f, 180f)

    private val eagleValue = ListValue("Eagle", arrayOf("Off", "Normal"), "Off")
    private val eagleEdgeDistanceValue = FloatRangeValue("EagleEdgeDistance", 0f, 0f, 0f, 0.2f) { !eagleValue.get().equals("off", true) }
    private val eagleBlocksValue = IntegerValue("EagleBlocks", 0, 1, 10) { eagleValue.get().equals("normal", true) }
    private val eagleSilent = BoolValue("Silent", false) { !eagleValue.get().equals("Off", true) }

    private val towerModeValue = ListValue("Tower", arrayOf("Off", "Air", "Legit", "MMC", "Matrix", "NCP", "Normal", "Vanilla", "Vulcan", "WatchDog"), "Off")
    private val sameYValue = ListValue("SameY", arrayOf("Off", "Same", "AutoJump"), "Off")

    private val timer = FloatValue("Timer", 1f, 0f, 5f)
    private val safeWalk = BoolValue("SafeWalk", false)
    private val jumpAutomatically = BoolValue("JumpAutomatically", false) {modes.get().equals("GodBridge", true)}
    private val movementCorrection = BoolValue("MovementCorrection", true)

    private val counter = BoolValue("Counter", false)

    /* Values */
    private var targetBlock: BlockPos? = null
    private var placeInfo: PlaceInfo? = null
    private var blockPlace: BlockPos? = null
    private var rotation: Rotation? = null
    private var xPos: Double = 0.0
    private var zPos: Double = 0.0
    private var startY = 0.0
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var ticksOnAir = 0

    // Render thingy
    private var progress = 0f
    private var lastMS = 0L

    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false
    private var itemStack: ItemStack? = null

    override fun onDisable() {
        mc.gameSettings.keyBindSneak.pressed = false
        mc.gameSettings.keyBindSprint.pressed = false
        mc.timer.timerSpeed = 1f
        RotationUtils.setRotations((mc.thePlayer as IEntityPlayerSP).serverRotation)
    }

    override fun onEnable() {
        targetYaw = mc.thePlayer.rotationYaw - 180
        targetPitch = 90f

        startY = floor(mc.thePlayer.posY)
        targetBlock = null
        lastMS = System.currentTimeMillis()
    }

    /* Init */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // jumpAutomatically only godbridge
         if (jumpAutomatically.get() && MovementUtils.isMoving && mc.thePlayer.onGround && !mc.thePlayer.isSneaking && !mc.gameSettings.keyBindSneak.isKeyDown && !mc.gameSettings.keyBindJump.isKeyDown &&
                mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.entityBoundingBox
                        .offset(0.0, -0.5, 0.0).expand(-0.001, 0.0, -0.001)).isEmpty())
            mc.thePlayer.jump()
            mc.rightClickMouse()
        // Eagle
        if (!eagleValue.equals("Off") && mc.thePlayer.onGround) {
            var dif = 0.5
            val edge = RandomUtils.nextFloat(eagleEdgeDistanceValue.getMinValue(), eagleEdgeDistanceValue.getMaxValue())
            val blockPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)

            if (edge > 0.0F) {
                for (facingType in StaticStorage.facings()) {
                    if (facingType == EnumFacing.UP || facingType == EnumFacing.DOWN) continue

                    val placeInfo = blockPos.offset(facingType)
                    if (BlockUtils.isReplaceable(blockPos)) {
                        var calcDif = if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH)
                            abs(placeInfo.z + 0.5 - mc.thePlayer.posZ)
                        else
                            abs(placeInfo.x + 0.5 - mc.thePlayer.posX)

                        calcDif -= 0.5

                        if (calcDif < dif)
                            dif = calcDif
                    }
                }
            }

            if (placedBlocksWithoutEagle >= eagleBlocksValue.get()) {
                val shouldEagle = BlockUtils.isReplaceable(blockPos) ||  (edge > 0 && dif < edge)

                if (eagleSilent.get()) {
                    if (eagleSneaking != shouldEagle) {
                        mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, if (shouldEagle) C0BPacketEntityAction.Action.START_SNEAKING else C0BPacketEntityAction.Action.STOP_SNEAKING))
                    }

                    eagleSneaking = shouldEagle
                } else {
                    mc.gameSettings.keyBindSneak.pressed = shouldEagle
                }

                placedBlocksWithoutEagle = 0
            } else {
                placedBlocksWithoutEagle++
            }
        }
    }
    @EventTarget
    fun onMove(event: MoveEvent) {
        event.isSafeWalk = safeWalk.get()
    }

    @EventTarget(priority = -1)
    fun onPreMotion(event: PreMotionEvent){
        if (targetBlock == null || placeInfo == null || blockPlace == null)
            return

        mc.timer.timerSpeed = timer.get()

        when (sprint.get().lowercase()) {
            "normal" -> {
                mc.gameSettings.keyBindSprint.pressed = true
            }
            "legit" -> {
                val player = MathHelper.wrapAngleTo180_float(MovementUtils.getPlayerDirection())
                val target = MathHelper.wrapAngleTo180_float(RotationUtils.targetRotation?.yaw ?: return)

                mc.gameSettings.keyBindSprint.pressed = if(RotationUtils.active) abs(player - target) > 90 - 22.5 else true
            }
            "omni" -> mc.thePlayer.isSprinting = true
            "vulcantoggle" -> {
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            }
            else -> {
                mc.gameSettings.keyBindSprint.pressed = false
                mc.thePlayer.isSprinting = false
            }
        }

        when(towerModeValue.get().lowercase()){
            "air" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.ticksExisted % 2 == 0 && blockNear(2)) {
                    mc.thePlayer.motionY = 0.42
                    mc.thePlayer.onGround = true
                }
            }

            "legit" ->{
                if (mc.thePlayer.onGround && mc.gameSettings.keyBindJump.isKeyDown) {
                    mc.thePlayer.jump()
                }
            }

            "matrix" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && isBlockUnder(2.0, false) && mc.thePlayer.motionY < 0.2) {
                    mc.thePlayer.motionY = 0.42
                    mc.thePlayer.onGround = true
                }
            }

            "ncp" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && blockNear(2)) {
                    PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(null))

                    if (mc.thePlayer.posY % 1 <= 0.00153598) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, floor(mc.thePlayer.posY), mc.thePlayer.posZ)
                        mc.thePlayer.motionY = 0.42
                    } else if (mc.thePlayer.posY % 1 < 0.1 && RotationUtils.offGroundTicks != 0) {
                        mc.thePlayer.motionY = 0.0
                        mc.thePlayer.setPosition(mc.thePlayer.posX, floor(mc.thePlayer.posY), mc.thePlayer.posZ)
                    }
                }
            }

            "normal" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.42
                    }
                }
            }

            "vanilla" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && blockNear(2)) {
                    mc.thePlayer.motionY = 0.42
                }
            }

            "vulcan" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && blockNear(2) && RotationUtils.offGroundTicks > 3) {
                    val itemStack = mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]

                    if (itemStack == null || (itemStack.stackSize > 2)) {
                        PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(null))
                    }
                    mc.thePlayer.motionY = 0.42
                }
            }

            "watchdog" -> {
                if (!mc.gameSettings.keyBindJump.isKeyDown || !MovementUtils.isMoving) {
                    return
                }

                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = MovementUtils.getJumpBoostModifier(0.42F)
                    mc.thePlayer.motionX *= .65
                    mc.thePlayer.motionZ *= .65
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        val packet = event.packet

        when(towerModeValue.get().lowercase()){
            "mmc" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && packet is C08PacketPlayerBlockPlacement) {
                    if (packet.getPosition() == BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY - 1.4,
                            mc.thePlayer.posZ
                        )
                    ) {
                        mc.gameSettings.keyBindSprint.pressed = false
                        mc.thePlayer.isSprinting = false
                        mc.thePlayer.motionY = 0.42
                    }
                }
            }

            "normal", "watchdog" -> {
                if (mc.thePlayer.motionY > -0.0784000015258789 && packet is C08PacketPlayerBlockPlacement) {
                    if (packet.getPosition() == BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY - 1.4,
                            mc.thePlayer.posZ
                        )
                    ) {
                        mc.thePlayer.motionY = -0.0784000015258789
                    }
                }
            }
        }

        if (packet is S2FPacketSetSlot) {
            if (packet.func_149174_e() == null) {
                event.cancelEvent()
                return
            }
            try {
                val slot = packet.func_149173_d() - 36
                if (slot < 0) return
                val itemStack = mc.thePlayer.inventory.getStackInSlot(slot)
                val item = packet.func_149174_e().item

                if ((itemStack == null && packet.func_149174_e().stackSize <= 6 && item is ItemBlock && item.block !in BLOCK_BLACKLIST) ||
                    (itemStack != null && abs(itemStack.stackSize - packet.func_149174_e().stackSize) <= 6) ||
                    (packet.func_149174_e() == null)
                ) {
                    event.cancelEvent()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    @EventTarget
    fun onTick(event: PreUpdateEvent){
        if (blockRelativeToPlayer(0, -1, 0) is BlockAir) ticksOnAir++ else ticksOnAir = 0

        /* Player Position Update (Edge Exception + Legit Mode) */
        xPos = mc.thePlayer.posX
        zPos = mc.thePlayer.posZ

        if(buildForward()) {
            xPos += mc.thePlayer.posX - mc.thePlayer.lastReportedPosX
            zPos += mc.thePlayer.posZ - mc.thePlayer.lastReportedPosZ
        }

        val blockSlot = InventoryUtils.findBlockInHotbar() ?: return
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot - 36
            itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).stack
        }

        search(this.getPlacePossibility() ?: return)?.let {
            placeInfo = it.placeInfo
            targetBlock = it.placeInfo.blockPos.offset(it.placeInfo.enumFacing)
            blockPlace = it.placeInfo.blockPos
            rotation = it.rotation
        }

        calculateRotations()

        if (sameYValue.get().equals("AutoJump", true)) {
            if(mc.thePlayer.onGround && (MovementUtils.isMoving || mc.gameSettings.keyBindJump.isPressed)){
                mc.thePlayer.jump()
            }
        }

        if (startY - 1 != floor(targetBlock?.y?.toDouble() ?: return) && sameY) {
            return
        }

        if (
            mc.thePlayer.inventory.currentItem == InventoryUtils.serverSlot &&
            !BadPacketUtils.bad(false, true, false, false, true) &&
            ticksOnAir > RandomUtils.nextInt(delayValue.getMinValue(), delayValue.getMaxValue())
        ) {
            place()
            ticksOnAir = 0
        }

        /**
         * SameY
         */
        if (mc.thePlayer.onGround || mc.gameSettings.keyBindJump.isKeyDown && !MovementUtils.isMoving) {
            startY = floor(mc.thePlayer.posY)
        }

        if(mc.thePlayer.posY < startY){
            startY = mc.thePlayer.posY
        }
    }

    private fun place() {
        when (modes.get().lowercase()){
            "legit" -> if(mc.thePlayer.posY < (mc.objectMouseOver.blockPos.y + 1.5)){
                if(mc.objectMouseOver.sideHit != EnumFacing.UP && mc.objectMouseOver.sideHit != EnumFacing.DOWN){
                    rightClickMouse()
                }
            } else if(mc.objectMouseOver.sideHit != EnumFacing.DOWN && mc.gameSettings.keyBindJump.isKeyDown){
                rightClickMouse()
            }
            else -> rightClickMouse()
        }
    }

    private fun rightClickMouse(){
        when(clicks.get().lowercase()){
            "legit" -> KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            "normal" -> mc.rightClickMouse()
            "raytraced" -> {
                if(isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)){
                    mc.rightClickMouse()
                }
            }
        }
    }

    /***
     * @author fmcpe
     *
     * calculateRealY
     * 6/21/2024
     */
    private fun calculateRealY(world: WorldClient, bb: AxisAlignedBB, motionX: Double, motionZ: Double, multiplier: Double, realY: Double = -0.8): Double {
        var y = realY
        val offset = bb.offset(motionX * multiplier, -0.08, motionZ * multiplier)
        val abb = bb.offset(motionX * multiplier, 0.0, motionZ * multiplier)

        for (box in world.getCollidingBoundingBoxes(mc.thePlayer, offset)) {
            y = box.calculateYOffset(abb, y)
        }

        return y
    }

    private fun getPlacePossibility(): BlockPos? {
        val possibilities = mutableListOf<Vec3>()

        for (x in -5..5) {
            for (y in -5..5) {
                for (z in -5..5) {
                    val block = BlockUtils.blockRelativeToPlayer(x, y, z)
                    if (block is BlockAir) continue
                    for (xAdd in -1..1 step 1){
                        for (yAdd in -1..1 step 1){
                            for (zAdd in -1..1 step 1){
                                if(xAdd != 0) possibilities.add(offset(x + xAdd, y, z))
                                if(yAdd != 0) possibilities.add(offset(x, y + yAdd, z))
                                if(zAdd != 0) possibilities.add(offset(x, y, z + zAdd))
                            }
                        }
                    }
                }
            }
        }

        possibilities.removeIf {
            mc.thePlayer.getDistance(it.xCoord, it.yCoord, it.zCoord) > 5 ||
                    mc.theWorld.getBlockState(BlockPos(it.xCoord, it.yCoord, it.zCoord)).block !is BlockAir
        }

        return BlockPos(
            possibilities.minByOrNull {
                val x = mc.thePlayer.posX - it.xCoord
                val y = mc.thePlayer.posY - 1 - it.yCoord
                val z = mc.thePlayer.posZ - it.zCoord
                sqrt(x * x + y * y + z * z)
            } ?: return null
        )
    }

    private fun offset(x: Int, y: Int, z: Int): Vec3{
        return Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z)
    }

    /**
     * Checks if the player is near a block
     *
     * @return block near
     */
    private fun blockNear(range: Int): Boolean {
        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    if (blockRelativeToPlayer(x, y, z) !is BlockAir) return true
                }
            }
        }

        return false
    }

    /**
     * Gets the block relative to the player from the offset
     *
     * @return block relative to the player
     */
    private fun blockRelativeToPlayer(offsetX: Int, offsetY: Int, offsetZ: Int): Block {
        return mc.theWorld.getBlockState(BlockPos(mc.thePlayer).add(offsetX, offsetY, offsetZ)).block
    }

    /**
     * Checks if there is a block under the player
     *
     * @return block under
     */
    private fun isBlockUnder(height: Double, boundingBox: Boolean): Boolean {
        if (boundingBox) {
            var offset = 0
            while (offset < height) {
                val bb = mc.thePlayer.entityBoundingBox.offset(0.0, -offset.toDouble(), 0.0)

                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isNotEmpty()) {
                    return true
                }
                offset += 2
            }
        } else {
            var offset = 0
            while (offset < height) {
                if (blockRelativeToPlayer(0, -offset, 0).isFullBlock) {
                    return true
                }
                offset++
            }
        }
        return false
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if(!movementCorrection.get()){
            MovementUtils.useDiagonalSpeed()
        }
    }

    @EventTarget
    fun onInput(event: MoveInputEvent){
        if (
            modes.get().equals("telly", true) &&
            mc.thePlayer.onGround &&
            MovementUtils.isMoving &&
            if(reset.get()) !RotationUtils.active else true
        ) {
            event.jump = true
        }
    }

    /**
     *  @return block relative to the player
     */
    private fun calculateRotations() {
        val fixes = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)

        /* Falling prediction */
        val c1 = calculateRealY(mc.theWorld, bb, motionX, motionZ, 0.546) <= -0.015625
        val a1 = calculateRealY(mc.theWorld, bb, motionX, motionZ, 1.0)
        val c2 = a1 <= -0.015625
        val c3 = calculateRealY(mc.theWorld, bb, motionX, motionZ, 1.09, a1) <= -0.015625

        when (modes.get().lowercase()) {
            "godbridge" -> {
                if (ticksOnAir > 0 && !isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)) {
                    val d = (mc.thePlayer.rotationYaw / 45).roundToInt()
                    val y = d * 45
                    targetYaw = if(d % 2 == 0) y - 135.0F else y - 180.0F
                    targetPitch = if(d % 2 == 0) 75.5F else 75.6F
                }
            }
            "snap" -> {
                getRotations()

                if (ticksOnAir <= 0 || isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)) {
                    targetYaw = fixes
                }
            }
            "legit" -> if (ticksOnAir > 0 && if (!mc.thePlayer.onGround) (mc.thePlayer.motionY > 0) else (c1 && c2 && c3) && !isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)) {
                if (
                    xPos > (blockPlace?.x?.plus(0.288) ?: return) ||
                    xPos < (blockPlace?.x?.minus(1.288) ?: return) ||
                    zPos > (blockPlace?.z?.plus(0.288) ?: return) ||
                    zPos < (blockPlace?.z?.minus(1.288) ?: return)
                ){
                    getRotations()
                }
            }
            "telly" -> {
                if (RotationUtils.offGroundTicks >= ticks.get()) {
                    if (!isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)) {
                        getRotations()
                    }
                } else {
                    if(reset.get()){
                        RotationUtils.active = false
                        return
                    } else {
                        getRotations()
                        targetYaw = mc.thePlayer.rotationYaw
                    }
                }
            }

            else -> if (ticksOnAir > 0 && !isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)) {
                getRotations()
            }
        }

        /* Setting rotations */
        RotationUtils.setRotations(
            Rotation(targetYaw, targetPitch),
            2,
            RandomUtils.nextFloat(speed.getMinValue(), speed.getMaxValue()),
            if (movementCorrection.get() && !modes.get().equals("none", true)) MovementFixType.FULL
            else MovementFixType.NONE,
            true,
            !modes.get().equals("none", true)
        )
    }

    /**
     * @author fmcpe
     *
     * 6/21/2024
     * Raytrace
     * From MCP
     */
    @JvmOverloads
    fun isObjectMouseOverBlock(
        facing: EnumFacing,
        block: BlockPos,
        rotation: Rotation? = currentRotation,
        obj: MovingObjectPosition? = rayTrace(rotation) ?: mc.objectMouseOver,
    ): Boolean{
        if (obj != null) {
            return obj.blockPos == block && obj.sideHit == facing
        }

        return false
    }

    private fun buildForward(realYaw: Float = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)): Boolean {
        return (realYaw in -77.5..77.5) && (realYaw in -102.5..102.5 || realYaw in -12.5..12.5 || realYaw < -167.5 || realYaw > 167.5)
    }

    private fun getRotations(){
        rotation?.let {
            targetYaw = it.yaw
            targetPitch = it.pitch
        }
    }

    private fun search(blockPos: BlockPos): PlaceRotation? {
        if (!BlockUtils.isReplaceable(blockPos)) return null
        var placeRotation: PlaceRotation? = null

        val playerEye = mc.thePlayer.eyes
        val maxReach = mc.playerController.blockReachDistance

        for (side in StaticStorage.facings()) {
            val neighborBlock = blockPos.offset(side)

            if (!BlockUtils.isClickable(neighborBlock))
                continue

            for (x in 0.1..0.9) {
                for (y in 0.1..0.9) {
                    for (z in 0.1..0.9) {
                        val currentPlaceRotation = findTargetPlace(blockPos, neighborBlock, Vec3(x, y, z), side, playerEye, maxReach) ?: continue

                        if (placeRotation == null || RotationUtils.getRotationDifference(currentPlaceRotation.rotation, currentRotation) < RotationUtils.getRotationDifference(placeRotation.rotation, currentRotation))
                            placeRotation = currentPlaceRotation
                    }
                }
            }
        }

        return placeRotation
    }

    private fun findTargetPlace(blockPosition: BlockPos, neighborBlock: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float): PlaceRotation? {
        mc.theWorld ?: return null

        val vec = (Vec3(blockPosition) + vec3).addVector(
            side.directionVec.x * vec3.xCoord, side.directionVec.y  * vec3.yCoord, side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (distance > maxReach || mc.theWorld.rayTraceBlocks(eyes, vec, false, true, false) != null)
            return null

        val rotation = RotationUtils.toRotation(vec, false)
        rotation.fixedSensitivity(mc.gameSettings.mouseSensitivity)

        // check raytrace
        rayTrace(currentRotation)?.let {
            if (it.blockPos == neighborBlock && it.sideHit == side.opposite) {
                val placeInfo = PlaceInfo(it.blockPos, side.opposite)

                return PlaceRotation(placeInfo, currentRotation)
            }
        }

        val raytrace = rayTrace(rotation) ?: return null

        return if (raytrace.blockPos == neighborBlock && raytrace.sideHit == side.opposite) {
            val placeInfo = PlaceInfo(raytrace.blockPos, side.opposite)
            PlaceRotation(placeInfo, rotation)
        } else null
    }

    private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3): Vec3 {
        val x = original.xCoord
        val y = original.yCoord
        val z = original.zCoord

        val side = direction.opposite

        return when (side.axis ?: return original) {
            EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
            EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
            EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
        }
    }

    /**
     * Render counter
     */
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        progress = (System.currentTimeMillis() - lastMS).toFloat() / 100F
        if (progress >= 1) progress = 1f

        val scaledResolution = ScaledResolution(mc)
        val info = "$blocksAmount Blocks"
        val infoWidth = Fonts.fontSFUI40.getStringWidth(info)

        if(counter.get()){
            GlStateManager.translate(0.0, (-14F - (progress * 4F)).toDouble(), 0.0)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glColor4f(0.15F, 0.15F, 0.15F, progress)
            GL11.glBegin(GL11.GL_TRIANGLE_FAN)
            GL11.glVertex2d((scaledResolution.scaledWidth / 2 - 3).toDouble(), (scaledResolution.scaledHeight - 60).toDouble())
            GL11.glVertex2d((scaledResolution.scaledWidth / 2).toDouble(), (scaledResolution.scaledHeight - 57).toDouble())
            GL11.glVertex2d((scaledResolution.scaledWidth / 2 + 3).toDouble(), (scaledResolution.scaledHeight - 60).toDouble())
            GL11.glEnd()
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
            RenderUtils.drawRoundedRect((scaledResolution.scaledWidth / 2 - (infoWidth / 2) - 4).toFloat(), (scaledResolution.scaledHeight - 60).toFloat(), (scaledResolution.scaledWidth / 2 + (infoWidth / 2) + 4).toFloat(), (scaledResolution.scaledHeight - 74).toFloat(), 2F, Color(0.15F, 0.15F, 0.15F, progress).rgb)
            GlStateManager.resetColor()
            Fonts.fontSFUI35.drawCenteredString(info, (scaledResolution.scaledWidth / 2).toFloat() + 0.1F, (scaledResolution.scaledHeight - 70).toFloat(), Color(1F, 1F, 1F, 0.8F * progress).rgb, false)
            GlStateManager.translate(0.0, (14F + (progress * 4F)).toDouble(), 0.0)
        }
    }

    /**
     * @return hotbar blocks amount
     */
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
                if (itemStack != null && itemStack.item is ItemBlock) {
                    val block = (itemStack.item as ItemBlock).getBlock()
                    if (!BLOCK_BLACKLIST.contains(block) && block.isFullCube) amount += itemStack.stackSize
                }
            }
            return amount
        }

    private val currentRotation: Rotation
        get() = RotationUtils.targetRotation ?: Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

    /**
     * @return sameY
     */
    private val sameY: Boolean
        get() = !sameYValue.get().equals("Off", true) && !mc.gameSettings.keyBindJump.isKeyDown && MovementUtils.isMoving
}

