/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.injection.forge.mixins.client;

import net.minusmc.minusbounce.MinusBounce;
import net.minecraft.client.settings.GameSettings;
import net.minusmc.minusbounce.event.*;
import org.spongepowered.asm.mixin.*;
import net.minusmc.minusbounce.injection.forge.mixins.client.MixinMovementInput;

import net.minecraft.client.Minecraft;
import net.minecraft.util.*;
import static net.minusmc.minusbounce.utils.RotationUtils.targetRotation;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptions extends MixinMovementInput{
    final Minecraft mc = Minecraft.getMinecraft();

    @Shadow
    @Final
    private GameSettings gameSettings;

    @Overwrite
    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;

        if (this.gameSettings.keyBindForward.isKeyDown()) {
            ++this.moveForward;
        }

        if (this.gameSettings.keyBindBack.isKeyDown()) {
            --this.moveForward;
        }

        if (this.gameSettings.keyBindLeft.isKeyDown()) {
            ++this.moveStrafe;
        }

        if (this.gameSettings.keyBindRight.isKeyDown()) {
            --this.moveStrafe;
        }

        this.jump = this.gameSettings.keyBindJump.isKeyDown();
        this.sneak = this.gameSettings.keyBindSneak.isKeyDown();

        final MoveInputEvent event = new MoveInputEvent(
            this.moveForward, 
            this.moveStrafe, 
            this.jump, 
            this.sneak, 
            0.3D,
            mc.thePlayer.rotationYaw,
            true
        );

        final float rotation = targetRotation != null ? targetRotation.getYaw() : event.getYaw();

        MinusBounce.eventManager.callEvent(event);

        final double sneakMultiplier = event.getSneakMultiplier();
        this.moveForward = event.getForward();
        this.moveStrafe = event.getStrafe();
        this.jump = event.getJump();
        this.sneak = event.getSneak();

        if(event.getCorrection()){
            final float offset = (float) Math.toRadians(mc.thePlayer.rotationYaw - rotation);
            
            final float cosValue = MathHelper.cos(offset);
            final float sinValue = MathHelper.sin(offset);

            this.moveForward = Math.round(this.moveForward * cosValue + this.moveStrafe * sinValue);
            this.moveStrafe = Math.round(this.moveStrafe * cosValue - this.moveForward * sinValue);
        }

        if (this.sneak) {
            this.moveStrafe = (float) ((double) this.moveStrafe * sneakMultiplier);
            this.moveForward = (float) ((double) this.moveForward * sneakMultiplier);
        }
    }
}
