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


import com.zhuyiren.rpc.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.Net;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.*;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public class DefaultServer implements Server {

    private List<ProviderInformation> services = new ArrayList<>();
    private NioEventLoopGroup worker;
    private EventExecutorGroup bussiness;
    private ServerBootstrap serverBootstrap;
    private ServerHandlerInitializer initializerHandler;

    private static final int DEFAULT_EVENT_LOOP_THREADS;
    private static final String DEFAULT_HOST="0.0.0.0";
    private static final int DEFAULT_PORT=3324;

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
        initializerHandler =new ServerHandlerInitializer(this);
        initServerBootstrap();
    }

    private void initServerBootstrap(){
        serverBootstrap=new ServerBootstrap();
        serverBootstrap.group(worker, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(initializerHandler);
    }

    @Override
    public boolean register(String serviceName, Object handler,String host,int port) {
        InetSocketAddress address=new InetSocketAddress(host,port);
        ProviderInformation provider = findProvider(address);
        if(provider==null){
            provider=new ProviderInformation(address);
            services.add(provider);
        }

        return provider.addService(serviceName,handler);
    }


    @Override
    public boolean register(String serviceName, Object handler, int port) {
        return register(serviceName,handler,DEFAULT_HOST,port);
    }

    @Override
    public boolean register(String serviceName, Object handler) {
        return register(serviceName,handler,DEFAULT_HOST,DEFAULT_PORT);
    }

    @Override
    public boolean start(SocketAddress address) throws IllegalArgumentException {
        if (!(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("监听地址无效");
        }

        ProviderInformation provider = findProvider(address);
        if(provider==null || provider.isStart()){
            return false;
        }

        try {
            serverBootstrap.bind(address).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("启动服务：");
            LOGGER.debug(provider.toString());
        }
        provider.setStart(true);
        return true;
    }

    @Override
    public boolean shutdown() {

        worker.shutdownGracefully();
        bussiness.shutdownGracefully();
        LOGGER.debug("Shutdown server");
        return true;
    }

    @Override
    public boolean start() {
        for (ProviderInformation provider : services) {
            start(provider.getAddress());
        }
        return true;
    }

    @Override
    public Map<String,Object> getServices(SocketAddress address) {
        ProviderInformation provider = findProvider(address);
        return provider.getServies();
    }

    @Override
    public EventExecutorGroup getBussinessExecutors() {
        return bussiness;
    }




    private ProviderInformation findProvider(SocketAddress address){
        for (ProviderInformation provider : services) {
            if(provider.getAddress().equals(address)){
                return provider;
            }
        }
        return null;
    }

}
