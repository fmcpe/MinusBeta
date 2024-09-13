package net.fmcpe.viaforge.api

import net.raphimc.vialoader.impl.viaversion.VLInjector
import net.raphimc.vialoader.netty.VLLegacyPipeline

class ProtocolVLInjector : VLInjector() {
    override fun getDecoderName(): String {
        return VLLegacyPipeline.VIA_DECODER_NAME
    }

    override fun getEncoderName(): String {
        return VLLegacyPipeline.VIA_ENCODER_NAME
    }
}