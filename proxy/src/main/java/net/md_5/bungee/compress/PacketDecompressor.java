package net.md_5.bungee.compress;

import lombok.*;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import net.md_5.bungee.jni.zlib.BungeeZlib;
import net.md_5.bungee.protocol.DefinedPacket;

@RequiredArgsConstructor
public class PacketDecompressor extends MessageToMessageDecoder<ByteBuf>
{

    private final int compressionThreshold;
    private final BungeeZlib zlib = CompressFactory.zlib.newInstance();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception
    {
        zlib.init( false, 0 );
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
        zlib.free();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        int decompressedSize = DefinedPacket.readVarInt( in );
        if ( decompressedSize == 0 )
        {
            out.add( in.slice().retain() );
            in.skipBytes( in.readableBytes() );
        } else
        {
            Preconditions.checkArgument(decompressedSize >= compressionThreshold, "Decompressed size %s less than compression threshold %s", decompressedSize, compressionThreshold);
            ByteBuf decompressed = ctx.alloc().directBuffer();

            try
            {
                zlib.process( in, decompressed );
                Preconditions.checkArgument( decompressed.readableBytes() == decompressedSize, "Decompressed size %s is not equal to decompressed bytes", decompressedSize, decompressed.readableBytes());

                out.add( decompressed );
                decompressed = null;
            } finally
            {
                if ( decompressed != null )
                {
                    decompressed.release();
                }
            }
        }
    }
}
