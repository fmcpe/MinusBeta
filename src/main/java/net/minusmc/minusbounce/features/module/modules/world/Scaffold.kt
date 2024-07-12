package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.client.entity.EntityPlayerSP
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
import net.minusmc.minusbounce.utils.extensions.plus
import net.minusmc.minusbounce.utils.misc.RandomUtils
import net.minusmc.minusbounce.utils.movement.MovementFixType
import net.minusmc.minusbounce.utils.render.RenderUtils
import net.minusmc.minusbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.*


@ModuleInfo("Scaffold", "Scaffold", "Use huge balls to rolling on mid-air", ModuleCategory.WORLD)
class Scaffold: Module(){

    private val modes = ListValue("Mode", arrayOf("Normal", "Snap", "Telly", "None", "GodBridge", "Legit"), "Normal")
    private val clicks = ListValue("ClickMode", arrayOf("Legit", "RayTraced", "Normal"), "RayTraced")
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
    private val movementCorrection = BoolValue("MovementCorrection", true)

    private val counter = BoolValue("Counter", false)

    /* Values */
    private var targetBlock: BlockPos? = null
    private var placeInfo: PlaceInfo? = null
    private var blockPlace: BlockPos? = null
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

        placeInfo = this.getPlacePossibility() ?: return
        targetBlock = placeInfo?.blockPos?.offset(placeInfo?.enumFacing)
        blockPlace = placeInfo?.blockPos ?: return

        calculateRotations()

        if (sameYValue.get().equals("AutoJump", true)) {
            mc.gameSettings.keyBindJump.pressed = mc.thePlayer.onGround && MovementUtils.isMoving || mc.gameSettings.keyBindJump.isPressed
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
            "normal" -> clickMouse()
            "raytraced" -> {
                if(isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)){
                    clickMouse()
                }
            }
        }
    }

    /***
     * @author Mojang
     *
     * From MCP
     */
    private fun clickMouse(){
        val itemStack = mc.thePlayer.inventory.getCurrentItem()
        var flag = true
        if (!mc.theWorld.isAirBlock(blockPlace)) {
            val i = itemStack?.stackSize ?: 0
            if (mc.playerController.onPlayerRightClick(
                    mc.thePlayer,
                    mc.theWorld,
                    itemStack,
                    blockPlace,
                    placeInfo?.enumFacing,
                    mc.objectMouseOver.hitVec
                )
            ) {
                mc.thePlayer.swingItem()
                flag = false
            }

            itemStack ?: return

            if (itemStack.stackSize == 0) {
                mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = null
            } else if (itemStack.stackSize != i || mc.playerController.isInCreativeMode) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress()
            }

            mc.sendClickBlockToController(mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown && mc.inGameHasFocus)
        }
        if (flag) {
            if (mc.playerController.sendUseItem(
                    mc.thePlayer,
                    mc.theWorld,
                    mc.thePlayer.inventory.getCurrentItem() ?: return
                )
            ) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress2()
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

    private fun getPlacePossibility(): PlaceInfo? {
        var placeRotation: PlaceInfo? = null

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

        for (side in StaticStorage.facings()) {
            val neighbor = BlockPos(
                possibilities.minByOrNull {
                    val x = mc.thePlayer.posX - it.xCoord
                    val y = mc.thePlayer.posY - 1 - it.yCoord
                    val z = mc.thePlayer.posZ - it.zCoord
                    sqrt(x * x + y * y + z * z)
                } ?: return null
            ).offset(side)

            if (BlockUtils.isClickable(neighbor)) {
                placeRotation = PlaceInfo(neighbor, side.opposite)
            }
        }

        return placeRotation
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
            MovementUtils.isMoving
        ) {
            event.jump = true
        }
    }

    /**
     *  @return block relative to the player
     */
    private fun calculateRotations() {
        /* Telly thingy*/
        val player = MovementUtils.getDirectionRotation(mc.thePlayer.rotationYaw, 0F, 1F).toFloat()
        val fixes = MathHelper.wrapAngleTo180_float(player)

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
                    targetYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)
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
                    getRotations()
                    targetYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)
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

    private fun getRotations(){
        val possible = mutableListOf<Rotation>()
        for(yaw in (mc.thePlayer.rotationYaw.toInt() - 180)..(mc.thePlayer.rotationYaw.toInt() + 180) step 45){
            for (pitch in 90 downTo 30){
                rayTrace(Rotation(yaw.toFloat(), pitch.toFloat()))?.let{
                    if(it.blockPos == blockPlace &&
                        it.sideHit == placeInfo?.enumFacing &&
                        it.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                        it.sideHit != EnumFacing.DOWN &&
                        it.blockPos.y <= (blockPlace?.y ?: return) &&
                        (it.sideHit != EnumFacing.UP || (!sameY && mc.gameSettings.keyBindJump.isKeyDown))
                    ){
                        possible.add(Rotation(yaw.toFloat(), pitch.toFloat()))
                    }
                }
            }
        }

        possible.minByOrNull {
            mc.thePlayer.getDistanceSq(
                rayTrace(
                    it
                )!!.hitVec + Vec3(0.2, 0.8, 0.2)
            ) + sqrt(
                (it.yaw - targetYaw).pow(2) + (it.pitch - targetPitch).pow(2)
            )
        }?.let {
            targetYaw = it.yaw
            targetPitch = it.pitch
        }
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
        rotation: Rotation? = null,
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

    /**
     * @return sameY
     */
    private val sameY: Boolean
        get() = !sameYValue.get().equals("Off", true) && !mc.gameSettings.keyBindJump.isKeyDown && MovementUtils.isMoving
}

private fun EntityPlayerSP.getDistanceSq(add: Vec3): Double = this.getDistanceSq(add.xCoord, add.yCoord, add.zCoord)
