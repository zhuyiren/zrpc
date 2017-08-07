package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.PacketDecoder;
import com.zhuyiren.rpc.common.PacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

/**
 * Created by zhuyiren on 2017/6/20.
 */
public class ClientHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private CallHandler callHandler;
    private static final boolean useZip;

    static {
        useZip = Boolean.parseBoolean(System.getProperty("useZip", "false"));
        System.out.println("useZip:" + useZip);
    }

    public ClientHandlerInitializer(CallHandler callHandler) {
        this.callHandler = callHandler;
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(204800000, 0, 4, 0, 4));
        if (useZip) {
            ch.pipeline().addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        }
        ch.pipeline().addLast(new PacketDecoder());

        ch.pipeline().addLast(new LengthFieldPrepender(4));
        if (useZip) {
            ch.pipeline().addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        }
        ch.pipeline().addLast(new PacketEncoder());
        RequestHandler sender = new RequestHandler(callHandler);
        callHandler.setCallWriter(sender);
        ch.pipeline().addLast(sender);
    }
}
