package com.zhuyiren.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by zhuyiren on 2017/6/9.
 */
public class HexDumpProxyInitializer  extends ChannelInitializer<SocketChannel>{


    private final String remoteHost;
    private final int remotePort;

    public HexDumpProxyInitializer(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                /*new LoggingHandler(LogLevel.INFO),*/
                new HexDumpProxyFrontendHandler(remoteHost, remotePort));
    }
}
