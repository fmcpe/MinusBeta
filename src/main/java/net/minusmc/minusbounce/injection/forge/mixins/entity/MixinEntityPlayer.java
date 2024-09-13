/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.injection.forge.mixins.entity;

import com.mojang.authlib.GameProfile;
import net.fmcpe.viaforge.api.McUpdatesHandler;
import net.fmcpe.viaforge.api.PacketManager;
import net.fmcpe.viaforge.api.ProtocolFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeHooks;
import net.minusmc.minusbounce.MinusBounce;
import net.minusmc.minusbounce.event.KnockBackEvent;
import net.minusmc.minusbounce.utils.ClientUtils;
import net.minusmc.minusbounce.utils.MinecraftInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minusmc.minusbounce.utils.MinecraftInstance.mc;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {

    @Shadow
    public abstract ItemStack getHeldItem();

    @Shadow
    public abstract GameProfile getGameProfile();

    @Shadow
    protected abstract boolean canTriggerWalking();

    @Shadow
    protected abstract String getSwimSound();

    @Shadow
    public abstract FoodStats getFoodStats();

    @Shadow
    public abstract ItemStack getCurrentEquippedItem();

    @Shadow
    protected int flyToggleTimer;

    @Shadow
    public PlayerCapabilities capabilities;

    @Shadow
    public abstract void onCriticalHit(Entity entityHit);

    @Shadow
    public abstract void onEnchantmentCritical(Entity entityHit);

    @Shadow
    public abstract void triggerAchievement(StatBase achievementIn);

    @Shadow
    public abstract int getItemInUseDuration();

    @Shadow
    public abstract void addExhaustion(float p_71020_1_);

    @Shadow
    public abstract ItemStack getItemInUse();

    @Shadow
    public abstract void destroyCurrentEquippedItem();

    @Shadow
    public abstract boolean isUsingItem();

    @Shadow
    public abstract void addStat(StatBase stat, int amount);

    /**
     * Attacks for the player the targeted entity with the currently equipped item.  The equipped item has hitEntity
     * called on it. Args: p_attackTargetEntityWithCurrentItem_1_
     * @author fmcpe
     * @reason KeepSprint
     */
    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("HEAD"), cancellable = true)
    public void attackTargetEntityWithCurrentItem(Entity p_attackTargetEntityWithCurrentItem_1_, CallbackInfo ci){
        if (ForgeHooks.onPlayerAttackTarget((EntityPlayer) (Object) this, p_attackTargetEntityWithCurrentItem_1_)) {
            if (p_attackTargetEntityWithCurrentItem_1_.canAttackWithItem() && !p_attackTargetEntityWithCurrentItem_1_.hitByEntity((Entity) (Object) this)) {
                float f = (float)this.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
                int i = 0;
                float f1;
                if (p_attackTargetEntityWithCurrentItem_1_ instanceof EntityLivingBase) {
                    f1 = EnchantmentHelper.getModifierForCreature(this.getHeldItem(), ((EntityLivingBase)p_attackTargetEntityWithCurrentItem_1_).getCreatureAttribute());
                } else {
                    f1 = EnchantmentHelper.getModifierForCreature(this.getHeldItem(), EnumCreatureAttribute.UNDEFINED);
                }

                i += EnchantmentHelper.getKnockbackModifier((EntityLivingBase) (Object) this);
                if (this.isSprinting()) {
                    ++i;
                }

                if (f > 0.0F || f1 > 0.0F) {
                    boolean flag = this.fallDistance > 0.0F && !this.onGround && !this.isOnLadder() && !this.isInWater() && !this.isPotionActive(Potion.blindness) && this.ridingEntity == null && p_attackTargetEntityWithCurrentItem_1_ instanceof EntityLivingBase;
                    if (flag && f > 0.0F) {
                        f *= 1.5F;
                    }

                    f += f1;
                    boolean flag1 = false;
                    int j = EnchantmentHelper.getFireAspectModifier((EntityLivingBase) (Object) this);
                    if (p_attackTargetEntityWithCurrentItem_1_ instanceof EntityLivingBase && j > 0 && !p_attackTargetEntityWithCurrentItem_1_.isBurning()) {
                        flag1 = true;
                        p_attackTargetEntityWithCurrentItem_1_.setFire(1);
                    }

                    double d0 = p_attackTargetEntityWithCurrentItem_1_.motionX;
                    double d1 = p_attackTargetEntityWithCurrentItem_1_.motionY;
                    double d2 = p_attackTargetEntityWithCurrentItem_1_.motionZ;
                    boolean flag2 = p_attackTargetEntityWithCurrentItem_1_.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) (Object) this), f);
                    if (flag2) {
                        KnockBackEvent event = new KnockBackEvent(0.6, false, 1, 0, false, false, false);
                        MinusBounce.eventManager.callEvent(event);
                        if (i > 0){
                            if(!event.isCancelled()){
                                for(int power = 0; power < event.getPower(); power++){
                                    p_attackTargetEntityWithCurrentItem_1_.addVelocity(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F, event.getReduceY() ? 0.0D : 0.1D, MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F);
                                }
                                this.motionX *= event.getMotion();
                                this.motionZ *= event.getMotion();
                                this.setSprinting(false);
                            }
                        } else if (event.getFull() && Minecraft.getMinecraft().thePlayer.hurtTime > 0) {
                            if(!event.isCancelled()){
                                for(int power = 0; power < event.getPower(); power++){
                                    p_attackTargetEntityWithCurrentItem_1_.addVelocity(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F, event.getReduceY() ? 0.0D : 0.1D, MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F);
                                }
                                this.motionX *= event.getMotion();
                                this.motionZ *= event.getMotion();
                                this.setSprinting(false);
                            }
                        }

                        if (event.getDebug()) ClientUtils.INSTANCE.displayChatMessage(
                                String.format(
                                        "Reduced %.3f %.3f",
                                        mc.thePlayer.motionX,
                                        mc.thePlayer.motionZ
                                )
                        );

                        if (p_attackTargetEntityWithCurrentItem_1_ instanceof EntityPlayerMP && p_attackTargetEntityWithCurrentItem_1_.velocityChanged) {
                            ((EntityPlayerMP)p_attackTargetEntityWithCurrentItem_1_).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(p_attackTargetEntityWithCurrentItem_1_));
                            p_attackTargetEntityWithCurrentItem_1_.velocityChanged = false;
                            p_attackTargetEntityWithCurrentItem_1_.motionX = d0;
                            p_attackTargetEntityWithCurrentItem_1_.motionY = d1;
                            p_attackTargetEntityWithCurrentItem_1_.motionZ = d2;
                        }

                        if (flag) {
                            this.onCriticalHit(p_attackTargetEntityWithCurrentItem_1_);
                        }

                        if (f1 > 0.0F) {
                            this.onEnchantmentCritical(p_attackTargetEntityWithCurrentItem_1_);
                        }

                        if (f >= 18.0F) {
                            this.triggerAchievement(AchievementList.overkill);
                        }

                        this.setLastAttacker(p_attackTargetEntityWithCurrentItem_1_);
                        if (p_attackTargetEntityWithCurrentItem_1_ instanceof EntityLivingBase) {
                            EnchantmentHelper.applyThornEnchantments((EntityLivingBase)p_attackTargetEntityWithCurrentItem_1_, (Entity) (Object) this);
                        }

                        EnchantmentHelper.applyArthropodEnchantments((EntityLivingBase) (Object) this, p_attackTargetEntityWithCurrentItem_1_);
                        ItemStack itemstack = this.getCurrentEquippedItem();
                        Entity entity = p_attackTargetEntityWithCurrentItem_1_;
                        if (p_attackTargetEntityWithCurrentItem_1_ instanceof EntityDragonPart) {
                            IEntityMultiPart ientitymultipart = ((EntityDragonPart)p_attackTargetEntityWithCurrentItem_1_).entityDragonObj;
                            if (ientitymultipart instanceof EntityLivingBase) {
                                entity = (EntityLivingBase)ientitymultipart;
                            }
                        }

                        if (itemstack != null && entity instanceof EntityLivingBase) {
                            itemstack.hitEntity((EntityLivingBase)entity, (EntityPlayer) (Object) this);
                            if (itemstack.stackSize <= 0) {
                                this.destroyCurrentEquippedItem();
                            }
                        }

                        if (p_attackTargetEntityWithCurrentItem_1_ instanceof EntityLivingBase) {
                            this.addStat(StatList.damageDealtStat, Math.round(f * 10.0F));
                            if (j > 0) {
                                p_attackTargetEntityWithCurrentItem_1_.setFire(j * 4);
                            }
                        }

                        this.addExhaustion(0.3F);
                    } else if (flag1) {
                        p_attackTargetEntityWithCurrentItem_1_.extinguish();
                    }
                }
            }
        }

        ci.cancel();
    }

    @Shadow
    public abstract boolean isPlayerSleeping();

    private final ItemStack[] mainInventory = new ItemStack[36];
    private final ItemStack[] armorInventory = new ItemStack[4];

    /**
     * @author As_pw
     * @reason Eye Height Fix
     */
    @Overwrite
    public float getEyeHeight() {
        final Minecraft mc = MinecraftInstance.mc;
        if (ProtocolFixer.newerThanOrEqualsTo1_13() && McUpdatesHandler.doingEyeRot)
            return McUpdatesHandler.lastEyeHeight + (McUpdatesHandler.eyeHeight - McUpdatesHandler.lastEyeHeight) * mc.timer.renderPartialTicks;
        if (this.isPlayerSleeping())
            return 0.2F;
        return PacketManager.lastEyeHeight + (PacketManager.eyeHeight - PacketManager.lastEyeHeight) * mc.timer.renderPartialTicks;
    }

    /**
     * @author As_pw
     * @reason 1.16+ Item Drop Fix
     */
    @Inject(method = "dropItem", at = @At("HEAD"))
    private void dropItem(ItemStack p_dropItem_1_, boolean p_dropItem_2_, boolean p_dropItem_3_, CallbackInfoReturnable<EntityItem> cir) {
        for (int i = 0; i < this.mainInventory.length; ++i) {
            if (ProtocolFixer.newerThanOrEqualsTo1_16())
                MinecraftInstance.mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
            if (this.mainInventory[i] != null) {
                this.mainInventory[i] = null;
            }
        }

        for (int j = 0; j < this.armorInventory.length; ++j) {
            if (ProtocolFixer.newerThanOrEqualsTo1_16())
                MinecraftInstance.mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
            if (this.armorInventory[j] != null) {
                this.armorInventory[j] = null;
            }
        }
    }

}