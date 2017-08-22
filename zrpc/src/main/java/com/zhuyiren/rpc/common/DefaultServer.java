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

package com.zhuyiren.rpc.common;


import com.zhuyiren.rpc.handler.ServerHandlerInitializer;
import com.zhuyiren.rpc.utils.ZookeeperUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
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


    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServer.class);


    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 3324;
    private static final String ZK_SERVICE_PREFIX="node";
    private static final int DEFAULT_IO_THREAD_SIZE = Runtime.getRuntime().availableProcessors();


    private List<ProviderInformation> services = new ArrayList<>();
    private NioEventLoopGroup nioExecutors;
    private EventExecutorGroup businessExecutors;
    private ServerBootstrap serverBootstrap;
    private ServerHandlerInitializer initializerHandler;
    private String host;
    private int port;
    private String zkConnectUrl;
    private CuratorFramework zkClient;
    private String zkNamespace;


    public DefaultServer(String zkConnectUrl,String namespace, String host, int port, int ioThreadSize, boolean useZip) {
        LOGGER.debug("Io thread size:" + ioThreadSize + ",businessExecutors thread size:" + ioThreadSize);
        nioExecutors = new NioEventLoopGroup(ioThreadSize);
        businessExecutors = new DefaultEventExecutorGroup(ioThreadSize);
        initializerHandler = new ServerHandlerInitializer(this, useZip);
        initServerBootstrap();
        this.host = host;
        this.port = port;
        this.zkConnectUrl = zkConnectUrl;
        this.zkNamespace=namespace;
        zkClient= CuratorFrameworkFactory.builder()
                .connectString(zkConnectUrl)
                .retryPolicy(new RetryNTimes(10,5000))
                .namespace(zkNamespace).build();
        zkClient.start();
        LOGGER.debug("Connect to zookeeper successfully");
    }


    public DefaultServer(String zkConnectUrl,String zkNamespace) {
        this(zkConnectUrl, zkNamespace,false);
    }


    public DefaultServer(String zkConnectUrl,String zkNamespace, boolean useZip) {
        this(zkConnectUrl, zkNamespace,DEFAULT_IO_THREAD_SIZE, useZip);
    }


    public DefaultServer(String zkConnectUrl,String zkNamespace, int ioThreadSize, boolean useZip) {
        this(zkConnectUrl,zkNamespace, DEFAULT_HOST, DEFAULT_PORT, ioThreadSize, useZip);
    }

    private void initServerBootstrap() {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(nioExecutors, nioExecutors)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(initializerHandler);
    }

    @Override
    public boolean register(String serviceName, Object handler, String host, int port) {
        if (!StringUtils.hasText(host)) {
            host = this.host;
        }
        if (port <= 0) {
            port = this.port;
        }
        InetSocketAddress address = new InetSocketAddress(host, port);
        ProviderInformation provider = findProvider(address);
        if (provider == null) {
            provider = new ProviderInformation(address);
            services.add(provider);
            start(provider.getAddress());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("启动服务：[服务地址:" + provider.getAddress() + "],[服务名称:" + serviceName + "],[处理类:" + handler + "]");
        }

        try {
            registerZookeeper(provider.getAddress(), serviceName);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
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

        nioExecutors.shutdownGracefully();
        businessExecutors.shutdownGracefully();
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
        return businessExecutors;
    }


    private void registerZookeeper(SocketAddress address, String serviceName) throws Exception {
        if (!(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("address must be InetSocketAddress");
        }
        String path="/"+serviceName.replaceAll("\\.","/");


        InetSocketAddress realAddress = (InetSocketAddress) address;
        String hostString = realAddress.getHostString();
        int port = realAddress.getPort();
        StringBuilder sb = new StringBuilder();
        sb.append(hostString).append(":").append(port);
        sb.append(":").append("0");

        if(writeZookeeperConfig(path, sb.toString())){
            LOGGER.debug("register service:[" + serviceName + "] to zookeeper");
        }
        return;
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


    private boolean writeZookeeperConfig(String path, String insertData) throws Exception {
        boolean isCreate = true;

        if (zkClient.checkExists().forPath(path) != null) {
            isCreate = false;
        }
        String nodePath=path+"/"+ZK_SERVICE_PREFIX;
        if(isCreate){
            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(nodePath,insertData.getBytes());
            return true;
        }
        List<String> childNames = zkClient.getChildren().forPath(path);
        for (String childName : childNames) {
            byte[] bytes = zkClient.getData().forPath(path + "/" + childName);
            String childData = new String(bytes);
            String originalAddress=childData.substring(0,childData.lastIndexOf(":"));
            String newAddress=insertData.substring(0,insertData.lastIndexOf(":"));
            if(originalAddress.equals(newAddress)){
                zkClient.delete().forPath(path+"/"+childName);
            }
        }

        zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(nodePath,insertData.getBytes());
        return true;

    }

    private class ZRpcWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {

        }
    }


}
