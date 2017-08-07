package com.zhuyiren.rpc.common;

import com.zhuyiren.rpc.engine.Writable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.OutputStream;

/**
 * Created by zhuyiren on 2017/4/16.
 */
public class PacketEncoder extends MessageToByteEncoder<Writable> {


    @Override
    protected void encode(ChannelHandlerContext ctx, Writable msg, ByteBuf out) throws Exception {
        OutputStream outputStream=new ByteBufOutputStream(out);
        msg.write(outputStream);
    }

}
