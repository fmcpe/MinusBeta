// longathelstan tech?
package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.player.Blink
import net.minusmc.minusbounce.features.module.modules.player.Blink.nullCheck
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.misc.FallingPlayer
import net.minusmc.minusbounce.utils.movement.MovementFixType
import net.minusmc.minusbounce.utils.render.RenderUtils
import net.minusmc.minusbounce.value.*
import org.lwjgl.opengl.GL11

@ModuleInfo(name = "AutoFireball", description = "Automatically place fireball behind the player.", category = ModuleCategory.WORLD)
class AutoFireball : Module() {
    private val movementCorrection = ListValue("MovementFix", arrayOf("None", "Normal", "Full"), "Full")
    private val rotationSpeed = FloatRangeValue("speed", 20f, 70f, 0f, 180f)
    private val pitchValue = FloatValue("pitch", 75.0f, -90.0f, 90.0f, " degrees")
    private val freezeDelay = IntegerValue("freeze-delay", 5, 0, 100, " ticks")
    private val fireballCooldown = IntegerValue("fireball-cooldown", 10, 0, 100, " ticks")
    private val addBlink = BoolValue("add-blink", false) // New toggle for Blink
    private val blinkPulse = BoolValue("blink-pulse", false) { addBlink.get() }
    private val pulseDelay = IntegerValue("pulse-delay", 1000, 0, 10000, "ms") { blinkPulse.get() && addBlink.get() }

    private var previousRotation: Rotation? = null
    private var previousMoveForward = 0f
    private var previousMoveStrafe = 0f
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var frozen = false
    private var freezeCounter = 0
    private var cooldownCounter = 0
    private var blinkActivationDelayCounter = 0 // Counter for delayed Blink activation
    private var blinkedPackets = ArrayList<Packet<*>>()
    private var blinkStartTime: Long = -1
    private var pos: Vec3? = null
    private var knockbackActive = false

    override fun onEnable() {
        if (mc.thePlayer != null) {
            x = mc.thePlayer.posX
            y = mc.thePlayer.posY
            z = mc.thePlayer.posZ
            previousRotation = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            previousMoveForward = mc.thePlayer.movementInput.moveForward
            previousMoveStrafe = mc.thePlayer.movementInput.moveStrafe
            frozen = true
            freezeCounter = freezeDelay.get()
            cooldownCounter = fireballCooldown.get()
            knockbackActive = false
        }
    }

    private fun startBlink() {
        blinkedPackets.clear()
        pos = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        blinkStartTime = System.currentTimeMillis()
    }

    private fun resetBlink() {
        synchronized(blinkedPackets) {
            for (packet in blinkedPackets) {
                PacketUtils.sendPacketNoEvent(packet)
            }
        }
        blinkedPackets.clear()
        pos = null
    }

    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (frozen) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionY = 0.0
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            freezeCounter--
            if (freezeCounter <= 0) frozen = false
        }

        if (blinkActivationDelayCounter > 0) {
            blinkActivationDelayCounter--
            if (blinkActivationDelayCounter == 0 && addBlink.get()) {
                startBlink() // Start Blink after 5-tick delay
            }
        }

        val fireballSlot = InventoryUtils.findFireballInHotbar() ?: run {
            ClientUtils.displayChatMessage("No Fireball found.")
            state = false
            return
        }

        mc.thePlayer.inventory.currentItem = fireballSlot - 36
        val rotationBehind = Rotation(MovementUtils.movingYaw - 180.0F, pitchValue.get())
        RotationUtils.setRotations(
            rotationBehind,
            speed = rotationSpeed.getMaxValue(),
            silent = true,
            fixType = when (movementCorrection.get().lowercase()) {
            "normal" -> MovementFixType.NORMAL
            "full" -> MovementFixType.FULL
            else -> MovementFixType.NONE
            }
        )


        if (cooldownCounter > 0) {
            cooldownCounter--
            return
        }

        val fireballPos = mc.thePlayer.positionVector.addVector(0.0, 0.0, -1.0)
        val hitVec = Vec3(fireballPos.xCoord, fireballPos.yCoord, fireballPos.zCoord)

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), BlockPos(hitVec), EnumFacing.DOWN, hitVec)) {
            mc.thePlayer.swingItem()
            mc.itemRenderer.resetEquippedProgress()
            knockbackActive = true
            state = false

            if (addBlink.get()) {
                blinkActivationDelayCounter = 5 + fireballCooldown.get() // Set the 5-tick delay for Blink
            }
        }

        if (knockbackActive && (mc.thePlayer.onGround || isVoid())) {
            knockbackActive = false
            if (addBlink.get()) resetBlink()
        }
    }

    @EventTarget
    fun onRenderWorld(e: Render3DEvent) {
        if (!nullCheck() || pos == null) {
            return
        }
        drawBox(pos!!)
    }

    @EventTarget
    fun onSendPacket(e: PacketEvent) {
        if (addBlink.get() && e.eventType == EventState.SEND) {
            if (!nullCheck()) {
                state = false
                return
            }
            val packet = e.packet
            if (packet.javaClass.simpleName.startsWith("S")) return
            if (packet is C00Handshake || packet is C00PacketLoginStart || packet is C00PacketServerQuery || packet is C01PacketEncryptionResponse || packet is C01PacketChatMessage) return

            blinkedPackets.add(packet)
            e.cancelEvent()

            if (blinkPulse.get() && System.currentTimeMillis() - blinkStartTime >= pulseDelay.get()) {
                resetBlink()
                startBlink()
            }
        }
    }

    private fun isVoid(): Boolean {
        return isFallingIntoVoid(mc.thePlayer)
    }

    private fun isFallingIntoVoid(player: EntityPlayer, ticks: Int = 100): Boolean {
        val fallingPlayer = FallingPlayer(player)
        val collisionResult = fallingPlayer.findCollision(ticks)
        return collisionResult == null
    }

    private fun drawBox(pos: Vec3) {
        GlStateManager.pushMatrix()
        val x: Double = pos.xCoord - mc.renderManager.viewerPosX
        val y: Double = pos.yCoord - mc.renderManager.viewerPosY
        val z: Double = pos.zCoord - mc.renderManager.viewerPosZ
        val bbox = mc.thePlayer.entityBoundingBox.expand(0.1, 0.1, 0.1)
        val axis = AxisAlignedBB(
            bbox.minX - mc.thePlayer.posX + x,
            bbox.minY - mc.thePlayer.posY + y,
            bbox.minZ - mc.thePlayer.posZ + z,
            bbox.maxX - mc.thePlayer.posX + x,
            bbox.maxY - mc.thePlayer.posY + y,
            bbox.maxZ - mc.thePlayer.posZ + z
        )
        val a = (Blink.color shr 24 and 255) / 255.0f
        val r = (Blink.color shr 16 and 255) / 255.0f
        val g = (Blink.color shr 8 and 255) / 255.0f
        val b = (Blink.color and 255) / 255.0f
        GL11.glBlendFunc(770, 771)
        GL11.glEnable(3042)
        GL11.glDisable(3553)
        GL11.glDisable(2929)
        GL11.glDepthMask(false)
        GL11.glLineWidth(2.0f)
        GL11.glColor4f(r, g, b, a)
        RenderUtils.drawBoundingBox(axis, r, g, b)
        GL11.glEnable(3553)
        GL11.glEnable(2929)
        GL11.glDepthMask(true)
        GL11.glDisable(3042)
        GlStateManager.popMatrix()
    }

    override fun onDisable() {
        frozen = false
        previousRotation = null
    }
}


