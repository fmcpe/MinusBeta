/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement.nowebs.other

import net.minusmc.minusbounce.event.MoveInputEvent
import net.minusmc.minusbounce.features.module.modules.movement.nowebs.NoWebMode


class IntaveNoWeb: NoWebMode("Intave") {
    private var wasInWeb = false
    private var counter = 0

    override fun onUpdate() {
        val player = mc.thePlayer
        if (player.isInWeb) {
            player.onGround = false

            player.jumpMovementFactor = when {
                !player.isCollidedVertically -> 0.8f

                player.movementInput.moveStrafe == 0.0f
                && mc.gameSettings.keyBindForward.isKeyDown
                && player.isCollidedVertically -> 0.74f

                else -> {
                    player.onGround = true
                    0.2f
                }
            }

            wasInWeb = player.isInWeb
        } else if (player.jumpMovementFactor > 0.03 && wasInWeb && !player.isInWeb) {
            wasInWeb = player.isInWeb
            player.jumpMovementFactor = 0.02f
        }
    }

    override fun onInput(event: MoveInputEvent) {
        if (mc.thePlayer.isCollidedVertically) {
            if (mc.thePlayer.isInWeb && event.strafe == 0.0f && mc.gameSettings.keyBindForward.isKeyDown) {
                event.forward = if (counter % 5 == 0) 0.0f else 1.0f
                counter++
            }
        } else {
            if (mc.thePlayer.isInWeb) {
                if (mc.thePlayer.isSprinting) {
                    event.forward = 0.0f
                }

                event.sneak = true
                event.forward = event.forward.coerceIn(-0.3f, 0.3f)
                event.strafe = if (event.forward == 0.0f) event.strafe.coerceIn(-0.3f, 0.3f) else 0.0f
            }
        }
    }

}