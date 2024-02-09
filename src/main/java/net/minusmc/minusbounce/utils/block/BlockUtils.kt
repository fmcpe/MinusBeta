/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils.block

import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.*
import net.minusmc.minusbounce.injection.access.StaticStorage
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.block.PlaceInfo.Companion.get
import net.minusmc.minusbounce.utils.extensions.*
import kotlin.math.*


object BlockUtils : MinecraftInstance() {

    /**
     * Get block from [blockPos]
     */
    @JvmStatic
    fun getBlock(blockPos: BlockPos?): Block? = mc.theWorld?.getBlockState(blockPos)?.block

    fun blockRelativeToPlayer(offsetX: Double, offsetY: Double, offsetZ: Double): Block {
        val playerPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        val offsetPos = playerPos.add(offsetX.toInt(), offsetY.toInt(), offsetZ.toInt())
        return mc.theWorld.getBlockState(offsetPos).block
    }

    @JvmStatic
    fun getBlock(vec3: Vec3): Block? = getBlock(BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))

    /**
     * Get material from [blockPos]
     */
    @JvmStatic
    fun getMaterial(blockPos: BlockPos?): Material? = getBlock(blockPos)?.material

    /**
     * Check [blockPos] is replaceable
     */
    @JvmStatic
    fun isReplaceable(blockPos: BlockPos?) = getMaterial(blockPos)?.isReplaceable ?: false

    /**
     * Get state from [blockPos]
     */
    @JvmStatic
    fun getState(blockPos: BlockPos?): IBlockState = mc.theWorld.getBlockState(blockPos)

    /**
     * Check if [blockPos] is clickable
     */
    @JvmStatic
    fun canBeClicked(blockPos: BlockPos?) = getBlock(blockPos)?.canCollideCheck(getState(blockPos), false) ?: false &&
            mc.theWorld.worldBorder.contains(blockPos)

    /**
     * Get block name by [id]
     */
    @JvmStatic
    fun getBlockName(id: Int): String = Block.getBlockById(id).localizedName

    /**
     * Check if block is full block
     */
    @JvmStatic
    fun isFullBlock(blockPos: BlockPos?): Boolean {
        val axisAlignedBB = getBlock(blockPos)?.getCollisionBoundingBox(mc.theWorld, blockPos, getState(blockPos))
                ?: return false
        return axisAlignedBB.maxX - axisAlignedBB.minX == 1.0 && axisAlignedBB.maxY - axisAlignedBB.minY == 1.0 && axisAlignedBB.maxZ - axisAlignedBB.minZ == 1.0
    }

    /**
     * Get distance to center of [blockPos]
     */
    @JvmStatic
    fun getCenterDistance(blockPos: BlockPos) =
            mc.thePlayer.getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

    /**
     * Search blocks around the player in a specific [radius]
     */
    @JvmStatic
    fun searchBlocks(radius: Int): Map<BlockPos, Block> {
        val blocks = mutableMapOf<BlockPos, Block>()

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(mc.thePlayer.posX.toInt() + x, mc.thePlayer.posY.toInt() + y,
                            mc.thePlayer.posZ.toInt() + z)
                    val block = getBlock(blockPos) ?: continue

                    blocks[blockPos] = block
                }
            }
        }

        return blocks
    }

    /**
     * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
     */
    @JvmStatic
    fun collideBlock(axisAlignedBB: AxisAlignedBB, collide: (Block?) -> Boolean): Boolean {
        for (x in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minX) until
                MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxX) + 1) {
            for (z in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minZ) until
                    MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxZ) + 1) {
                val block = getBlock(BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble()))

                if (!collide(block))
                    return false
            }
        }

        return true
    }

    /**
     * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
     */
    @JvmStatic
    fun collideBlockIntersects(axisAlignedBB: AxisAlignedBB, collide: (Block?) -> Boolean): Boolean {
        for (x in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minX) until
                MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxX) + 1) {
            for (z in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minZ) until
                    MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxZ) + 1) {
                val blockPos = BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble())
                val block = getBlock(blockPos)

                if (collide(block)) {
                    val boundingBox = block?.getCollisionBoundingBox(mc.theWorld, blockPos, getState(blockPos))
                            ?: continue

                    if (mc.thePlayer.entityBoundingBox.intersectsWith(boundingBox))
                        return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun floorVec3(vec3: Vec3) = Vec3(floor(vec3.xCoord),floor(vec3.yCoord),floor(vec3.zCoord))

    private val blockNames = mutableListOf<Pair<String, Int>>()
    fun getBlockNamesAndIDs(): Array<Pair<String, Int>> {
        if (blockNames.isEmpty()) {
            for (id in 0..32768) { // arbitrary
                val block = Block.getBlockById(id)
                if (block === Blocks.air) continue

                blockNames.add(block.registryName.replace(Regex("^minecraft:"), "") to id)
            }
            blockNames.sortBy { it.first }
        }
        return blockNames.toTypedArray()
    }

    fun getBlockName2(id: Int): String {
        return Block.getBlockById(id).registryName.replace(Regex("^minecraft:"), "")
    }

    /**
     * Search rotation from blockPos.
     *
     * @author ccbluex
     */
    fun BlockPos.getRotations(): PlaceRotation? {
        val eyesPos = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)

        var placeRotation: PlaceRotation? = null

        for (side in StaticStorage.facings()) {
            val neighbor = this.offset(side)
            if (!canBeClicked(neighbor)) continue

            val dirVec = Vec3(side.directionVec)

            for (x in 0.1..0.9){
                for (y in 0.1..0.9){
                    for (z in 0.1..0.9){
                        val posVec = Vec3(this).addVector(x, y, z)
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))

                        if ((eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)) {
                            continue
                        }
                        val diffX = hitVec.xCoord - eyesPos.xCoord
                        val diffY = hitVec.yCoord - eyesPos.yCoord
                        val diffZ = hitVec.zCoord - eyesPos.zCoord
                        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
                        val rotation = Rotation(
                            MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                            MathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, diffXZ))).toFloat())
                        )
                        val rotationVector = RotationUtils.getVectorForRotation(rotation)
                        val vector = eyesPos.addVector(
                            rotationVector.xCoord * 4,
                            rotationVector.yCoord * 4,
                            rotationVector.zCoord * 4
                        )
                        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true) ?: continue
                        if (obj.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || obj.blockPos != neighbor || obj.sideHit != side.opposite) {
                            continue
                        }
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.rotation))
                            placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                    }
                }
            }
        }
        return placeRotation
    }

    fun getRot(pos: BlockPos): PlaceRotation? = pos.getRotations()

    /**
     * Finding correct yaw and pitch for placing a block
     * @author fmcpe, toidicakhia
     */
    fun getPlace(pos: BlockPos, yaw: Float): PlaceRotation? {
        val info = get(pos) ?: return null
        val blockFace = info.blockPos
        val side = info.enumFacing
        var placeRotation: PlaceRotation? = null

        for (pitch in -90.0..90.0 step 0.05) {
            val rotation = Rotation(mc.thePlayer.rotationYaw - yaw, pitch.toFloat())

            if (!rayCast(blockFace, side, true, rotation)) continue

            if (placeRotation == null || rotation.pitch < placeRotation.rotation.pitch) {
                placeRotation = PlaceRotation(
                    PlaceInfo(blockFace, side),
                    rotation
                )
            }
        }

        return placeRotation
    }

//    fun getFacing(input: BlockPos, neighbor: BlockPos): EnumFacing {
//        return when {
//            input.x < neighbor.x -> EnumFacing.WEST
//            input.x > neighbor.x -> EnumFacing.EAST
//            input.z < neighbor.z -> EnumFacing.NORTH
//            input.z > neighbor.z -> EnumFacing.SOUTH
//            else -> EnumFacing.UP
//        }
//    }

//
//    fun getBlockData(input: BlockPos): PlaceInfo? {
//        val blacklist = listOf(Blocks.AIR, Blocks.WATER, Blocks.LAVA)
//
//        val positions = listOf(
//            input.add(0, -1, 0),
//            input.add(-1, 0, 0),
//            input.add(1, 0, 0),
//            input.add(0, 0, -1),
//            input.add(0, 0, 1),
//            input.add(1, 0, 1),
//            input.add(-1, 0, -1)
//        )
//
//        return positions
//            .firstOrNull { !mc.theWorld.getBlockState(it).block.isReplaceable(mc.theWorld, it) || !blacklist.contains(mc.theWorld.getBlockState(it).block)}?
//            .let { PlaceInfo(it, getFacing(input, it)) }
//    }

    /**
     * Checking if the rotation is correct from blockPos and facing.
     * 
     * @author fmcpe
     */
    @JvmOverloads
    fun rayCast(
        pos: BlockPos?,
        facing: EnumFacing?,
        check: Boolean,
        rotation: Rotation? = null
    ): Boolean {
        val obj = if(rotation != null) {
            distanceRayTrace(rotation)
        } else {
            mc.objectMouseOver
        }

        pos ?: return false
        facing ?: return false

        return obj.blockPos == pos && (obj.sideHit == facing || !check)
    }

    /**
     * Raytrace from a rotation.
     * 
     * @author fmcpe
     */
    @JvmStatic
    fun distanceRayTrace(rotation: Rotation): MovingObjectPosition {
        val vec = RotationUtils.getVectorForRotation(rotation)
        val vector = eyesPos.addVector(vec.xCoord * 4.5, vec.yCoord * 4.5, vec.zCoord * 4.5)
        return mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)
    }

    /**
     * Check if [blockPos] is clickable.
     * 
     * @author fmcpe
     */
    fun BlockPos.isClickable() =
        getBlock(this)?.canCollideCheck(getState(this), false) ?: false && mc.theWorld.worldBorder.contains(this)

    /**
     * Eyes position.
     * 
     * @author fmcpe
     */
    val eyesPos: Vec3
        get() = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)

    val lastPos: Vec3
        get() = Vec3(mc.thePlayer.lastReportedPosX, mc.thePlayer.lastReportedPosY + mc.thePlayer.eyeHeight, mc.thePlayer.lastReportedPosZ)
}
