/*
 * Copyright 2017 The ZRPC Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhuyiren.rpc;


import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public class DefaultServer implements Server {

    private Map<String, Object> services = new HashMap<>();
    private NioEventLoopGroup worker;
    private EventExecutorGroup bussiness;
    private ServerBootstrap serverBootstrap;
    private ServerHandlerInitializer pipeHandler;

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    private static final Logger LOGGER= LoggerFactory.getLogger(DefaultServer.class);

    static {
        DEFAULT_EVENT_LOOP_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    }

    public DefaultServer(){
        this(0);
    }


    public DefaultServer(int size) {
        LOGGER.debug("Io thread size:"+size+",bussiness threas size:"+size);
        worker=new NioEventLoopGroup(size);
        bussiness=new DefaultEventExecutorGroup(size);
        pipeHandler=new ServerHandlerInitializer(this);
        initServerBootstrap();
    }

    private void initServerBootstrap(){
        serverBootstrap=new ServerBootstrap();
        serverBootstrap.group(worker, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(pipeHandler);

    }

    @Override
    public void register(String serviceName, Object handler) {
        services.put(serviceName, handler);
    }

    @Override
    public void start(final int port) {
        try {
            serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.debug("Start server on port:"+port);
    }

    @Override
    public void shutdown() {

        worker.shutdownGracefully();
        bussiness.shutdownGracefully();
        LOGGER.debug("Shutdown server");
    }

    @Override
    public Map<String, Object> getServices() {
        return services;
    }

    @Override
    public EventExecutorGroup getBussinessExecutors() {
        return bussiness;
    }


    @Override
    public void registerEngine(Engine engine) {

    }
}
