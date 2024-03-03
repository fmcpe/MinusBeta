/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils.block

import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.util.*
import net.minusmc.minusbounce.injection.access.StaticStorage
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.RotationUtils.targetRotation
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
    fun isClickable(blockPos: BlockPos?) = getBlock(blockPos)?.canCollideCheck(getState(blockPos), false) ?: false &&
            mc.theWorld.worldBorder.contains(blockPos)

    fun air(pos: BlockPos) = getBlock(pos) is BlockAir
    fun air(pos: Vec3) = getBlock(pos) is BlockAir
    fun air(x: Double, y: Double, z: Double) = getBlock(BlockPos(x, y, z)) is BlockAir
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
            if (!isClickable(neighbor)) continue

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
                        val rotation = RotationUtils.toRotation(hitVec, false)
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
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.rotation)) {
                            placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                        }
                    }
                }
            }
        }
        return placeRotation
    }

    /**
     * Checking if the rotation is correct from blockPos and facing.
     * 
     * @author fmcpe
     */
    @JvmOverloads
    fun rayCast(
        pos: BlockPos?,
        facing: EnumFacing?,
        obj: MovingObjectPosition? = mc.objectMouseOver
    ): Boolean {
        obj ?: return false
        pos ?: return false
        facing ?: return false

        return obj.blockPos == pos && obj.sideHit == facing
    }

    /**
     * Thanks, Grim!
     *
     * I don't know ?
     * @author fmcpe
     * @author MWHunter
     */
    fun calculateDirection(xRot: Float, yRot: Float): Vec3 {
        val rotX = xRot * Math.PI / 180f
        val rotY = yRot * Math.PI / 180f

        return Vec3(-cos(rotY) * sin(rotX), -sin(rotY), cos(rotY) * cos(rotX))
    }

    fun getPointAtDistance(direction: Vec3, origin: Vec3, distance: Double): Vec3 {
        val dir = Vec3(direction.xCoord, direction.yCoord, direction.zCoord)
        val orig = Vec3(origin.xCoord, origin.yCoord, origin.zCoord)
        return orig + (dir * distance)
    }

    fun getHitVec(place: BlockPos): MovingObjectPosition? = AxisAlignedBB(place, place + 1).calculateIntercept(eyesPos, getPointAtDistance(calculateDirection(targetRotation!!.yaw, targetRotation!!.pitch), eyesPos, 6.0))

    /**
     * Raytrace from a rotation.
     * 
     * @author fmcpe
     */
    @JvmOverloads
    fun distanceRayTrace(rotation: Rotation?, range: Double = 4.5): MovingObjectPosition? {
        rotation ?: return mc.objectMouseOver

        val vec = RotationUtils.getVectorForRotation(rotation)
        val vector = eyesPos.addVector(vec.xCoord * range, vec.yCoord * range, vec.zCoord * range)
        return mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)
    }

    /**
     * Eyes position.
     * 
     * @author fmcpe
     */
    private val eyesPos: Vec3
        get() = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
}
