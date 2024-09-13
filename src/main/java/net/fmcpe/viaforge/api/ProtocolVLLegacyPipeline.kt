package net.fmcpe.viaforge.api

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import net.raphimc.vialoader.netty.VLLegacyPipeline

class ProtocolVLLegacyPipeline(user: UserConnection?, version: ProtocolVersion?) : VLLegacyPipeline(user, version) {
    override fun decompressName(): String {
        return "decompress"
    }

    override fun compressName(): String {
        return "compress"
    }

    override fun packetDecoderName(): String {
        return "decoder"
    }

    override fun packetEncoderName(): String {
        return "encoder"
    }

    override fun lengthSplitterName(): String {
        return "splitter"
    }

    override fun lengthPrependerName(): String {
        return "prepender"
    }
}