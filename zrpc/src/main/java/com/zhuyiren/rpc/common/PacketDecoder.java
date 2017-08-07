package com.zhuyiren.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.InputStream;
import java.util.List;

/**
 * Created by zhuyiren on 2017/4/16.
 */
public class PacketDecoder extends ByteToMessageDecoder {



    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        InputStream inputStream = new ByteBufInputStream(in);
        Packet packet = new Packet();
        packet.readFields(inputStream);
        out.add(packet);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        System.out.println("error");
        // cause.printStackTrace();
    }
}
