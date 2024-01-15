/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils.block

import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.block.PlaceInfo.Companion.get
import net.minusmc.minusbounce.injection.access.StaticStorage
import net.minusmc.minusbounce.utils.extensions.*
import net.minecraft.init.Blocks
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.util.*
import kotlin.math.*

object BlockUtils : MinecraftInstance() {

    /**
     * Get block from [blockPos]
     */
    @JvmStatic
    fun getBlock(blockPos: BlockPos?): Block? = mc.theWorld?.getBlockState(blockPos)?.block

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
     * Finding corrent yaw and pitch to placing block
     * 
     * @author fmcpe
     */
    @JvmStatic
    fun searchBlock(blockPosition: BlockPos, yaw: Float, checks: Boolean): PlaceRotation? {
        val eyesPos = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
        val fromPos = fromPos(blockPosition)

        var placeRotation: PlaceRotation? = null

        for (side in StaticStorage.facings()) {
            val neighbor = blockPosition.offset(side)
            val dirVec = Vec3(side.directionVec)

            cantClick(neighbor) ?: continue

            for (pitch in -90.0..90.0 step 0.02) {
                val rotation = Rotation(mc.thePlayer.rotationYaw - yaw, pitch.toFloat())

                if (fromBlocks(rotation, neighbor, side.opposite) != null) {
                    if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.rotation))
                        placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, rayTrace(rotation).hitVec), rotation)
                    
                    break
                }
            }

            if(placeRotation == null && fromPos != null)
                placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, rayTrace(fromPos).hitVec), fromPos)
        }

        return placeRotation
    }

    /**
     * Finding if the rotation is correct from blockPos and facing
     * 
     * @author fmcpe
     */
    @JvmStatic
    fun fromBlocks(rotation: Rotation, pos: BlockPos, facing: EnumFacing): Boolean? {
        val eyesPos = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)

        val vec = RotationUtils.getVectorForRotation(rotation)
        val vector = eyesPos.addVector(vec.xCoord * 4.5, vec.yCoord * 4.5, vec.zCoord * 4.5)
        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)

        return if(
                obj.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || 
                obj.blockPos != pos || 
                obj.sideHit != facing
            ) null else false
    }

    /**
     * Raytrace from a rotation
     * 
     * @author fmcpe
     */
    @JvmStatic
    fun rayTrace(rotation: Rotation): MovingObjectPosition {
        val eyesPos = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
        val vec = RotationUtils.getVectorForRotation(rotation)
        val vector = eyesPos.addVector(vec.xCoord * 4.5, vec.yCoord * 4.5, vec.zCoord * 4.5)
        return mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)
    }

    /**
     * Search rotation from blockPos
     * 
     * @author fmcpe
     */
    @JvmStatic
    fun fromPos(blockPosition: BlockPos): Rotation? {
        val blockData = get(blockPosition) ?: return null

        return RotationUtils.getDirectionToBlock(blockData.blockPos.x.toDouble(), blockData.blockPos.y.toDouble(), blockData.blockPos.z.toDouble(), blockData.enumFacing)
    }

    /**
     * Check if [blockPos] is not clickable
     * 
     * @author fmcpe
     */
    @JvmStatic
    fun cantClick(blockPos: BlockPos?): Boolean? = 
        if(getBlock(blockPos)?.canCollideCheck(getState(blockPos), false) ?: false &&
            mc.theWorld.worldBorder.contains(blockPos)) false else null
}
