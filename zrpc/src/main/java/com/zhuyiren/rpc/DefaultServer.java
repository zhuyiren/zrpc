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


import com.zhuyiren.rpc.handler.ServerHandlerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public class DefaultServer implements Server {

    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 3324;
    private static final int DEFAULT_IO_THREAD_SIZE=Runtime.getRuntime().availableProcessors();


    private List<ProviderInformation> services = new ArrayList<>();
    private NioEventLoopGroup worker;
    private EventExecutorGroup business;
    private ServerBootstrap serverBootstrap;
    private ServerHandlerInitializer initializerHandler;
    private String host;
    private int port;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServer.class);

    public DefaultServer(String host, int port, int ioThreadSize,boolean useZip) {
        LOGGER.debug("Io thread size:" + ioThreadSize + ",business thread size:" + ioThreadSize);
        worker = new NioEventLoopGroup(ioThreadSize);
        business = new DefaultEventExecutorGroup(ioThreadSize);
        initializerHandler = new ServerHandlerInitializer(this,useZip);
        initServerBootstrap();
        this.host=host;
        this.port=port;
    }


    public DefaultServer() {
        this(false);
    }


    public DefaultServer(boolean useZip){
        this(DEFAULT_IO_THREAD_SIZE,useZip);
    }


    public DefaultServer(int ioThreadSize,boolean useZip) {
        this(DEFAULT_HOST,DEFAULT_PORT,ioThreadSize,useZip);
    }

    private void initServerBootstrap() {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(worker, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(initializerHandler);
    }

    @Override
    public boolean register(String serviceName, Object handler, String host, int port) {
        if(!StringUtils.hasText(host)){
            host=this.host;
        }
        if(port<=0){
            port=this.port;
        }
        InetSocketAddress address = new InetSocketAddress(host, port);
        ProviderInformation provider = findProvider(address);
        if (provider == null) {
            provider = new ProviderInformation(address);
            services.add(provider);
            start(provider.getAddress());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("启动服务：[服务地址:"+provider.getAddress()+"],[服务名称:"+serviceName+"],[处理类:"+handler+"]");
        }
        return provider.addService(serviceName, handler);
    }


    @Override
    public boolean register(String serviceName, Object handler, int port) {
        return register(serviceName, handler, host, port);
    }

    @Override
    public boolean register(String serviceName, Object handler) {
        return register(serviceName, handler, host, port);
    }

    @Override
    public boolean start(SocketAddress address) throws IllegalArgumentException {
        if (!(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("监听地址无效");
        }

        ProviderInformation provider = findProvider(address);
        if (provider == null || provider.isStart()) {
            return false;
        }

        try {
            serverBootstrap.bind(address).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        provider.setStart(true);
        return true;
    }

    @Override
    public boolean shutdown() {

        worker.shutdownGracefully();
        business.shutdownGracefully();
        LOGGER.debug("Shutdown server");
        return true;
    }


    @Override
    public Map<String, Object> getServices(SocketAddress address) {
        ProviderInformation provider = findProvider(address);
        return provider.getServices();
    }

    @Override
    public EventExecutorGroup getBusinessExecutors() {
        return business;
    }


    private ProviderInformation findProvider(SocketAddress address) {
        SocketAddress anyAddress = new InetSocketAddress("0.0.0.0", ((InetSocketAddress) address).getPort());
        for (ProviderInformation provider : services) {
            if (provider.getAddress().equals(anyAddress)) {
                return provider;
            }
        }

        for (ProviderInformation provider : services) {
            if (provider.getAddress().equals(address)) {
                return provider;
            }
        }

        return null;
    }

}
