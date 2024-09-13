package net.minusmc.minusbounce.injection.forge.mixins.packets;

import net.fmcpe.viaforge.api.ProtocolFixer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(C0FPacketConfirmTransaction.class)
public class MixinC0FPacketConfirmTransaction {

    @Shadow
    public int windowId;
    @Shadow
    public short uid;
    @Shadow
    private boolean accepted;

    /**
     * @author FlorianMichael
     * @reason 1.17+ Transaction Fix
     */
    @Overwrite
    public void writePacketData(PacketBuffer buf) {
        if (ProtocolFixer.newerThanOrEqualsTo1_17())
            buf.writeInt(this.windowId);
        else {
            buf.writeByte(this.windowId);
            buf.writeShort(this.uid);
            buf.writeByte(this.accepted ? 1 : 0);
        }
    }
}