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


import com.google.common.base.Strings;
import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import com.zhuyiren.rpc.handler.RandomLoadBalanceStrategy;
import com.zhuyiren.rpc.handler.ServerHandlerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.zhuyiren.rpc.common.ZRpcPropertiesConstant.ANY_HOST;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public class DefaultServer implements Server {


    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServer.class);


    private static final String ZK_SERVICE_PREFIX = "node";
    private static final int DEFAULT_IO_THREAD_SIZE = Runtime.getRuntime().availableProcessors();


    private List<ProviderInformation> services = new CopyOnWriteArrayList<>();
    private NioEventLoopGroup nioExecutors;
    private EventExecutorGroup businessExecutors;
    private ServerBootstrap serverBootstrap;
    private ServerHandlerInitializer initializerHandler;
    private String host;
    private int port;
    private String zkConnectUrl;
    private CuratorFramework zkClient;
    private String zkNamespace;
    private int ioThreadSize;
    private boolean useZip;
    private List<Engine> engines;


    private boolean init() {
        LOGGER.debug("Io thread size:" + ioThreadSize + ",businessExecutors thread size:" + ioThreadSize);
        nioExecutors = new NioEventLoopGroup(ioThreadSize);
        businessExecutors = new DefaultEventExecutorGroup(ioThreadSize);
        if (engines == null || engines.size() == 0) {
            initEngines();
        }
        initializerHandler = new ServerHandlerInitializer(this, engines, useZip);
        initServerBootstrap();
        if (!Strings.isNullOrEmpty(zkConnectUrl)) {
            zkClient = CuratorFrameworkFactory.builder()
                    .connectString(zkConnectUrl)
                    .retryPolicy(new RetryNTimes(10, 5000))
                    .namespace(zkNamespace).build();
            zkClient.start();
            LOGGER.debug("Connect to zookeeper successfully");
        }
        return true;
    }

    private DefaultServer() {

    }

    private void initServerBootstrap() {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(nioExecutors, nioExecutors)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(initializerHandler);
    }

    @Override
    public boolean register(String serviceName, Object handler, String type, String host, int port) {
        if (Strings.isNullOrEmpty(host)) {
            host = this.host;
        }
        if (port <= 0) {
            port = this.port;
        }
        if (ANY_HOST.equals(host)) {
            throw new IllegalArgumentException("The provider host must not be 0.0.0.0");
        }
        if (type == null) {
            LOGGER.warn("The load balance type is null,and will use random type by default");
            type = RandomLoadBalanceStrategy.LOAD_BALANCE_TYPE;
        }
        InetSocketAddress address = new InetSocketAddress(host, port);
        ProviderInformation provider = findProvider(address);
        if (provider == null) {
            provider = new ProviderInformation(address);
            services.add(provider);
            start(provider.getAddress());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("启动服务：[服务地址:" + provider.getAddress() + "],[服务名称:" + serviceName + "],[处理类:" + handler + "] loadBalanceType:[" + type + "]");
        }

        try {
            registerZookeeper(provider.getAddress(), serviceName, type);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        return provider.addService(serviceName, handler);
    }


    @Override
    public boolean register(String serviceName, Object handler) {
        return register(serviceName, handler, RandomLoadBalanceStrategy.LOAD_BALANCE_TYPE);
    }

    @Override
    public boolean register(String serviceName, Object handler, String type) {
        return register(serviceName, handler, type, host, port);
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
        return provider == null ? new HashMap<>() : provider.getServices();
    }

    @Override
    public EventExecutorGroup getBusinessExecutors() {
        return businessExecutors;
    }


    private void registerZookeeper(SocketAddress address, String serviceName, String type) throws Exception {

        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            LOGGER.warn("The server is not registering service information to zookeeper,because it not connecting the zookeeper");
            return;
        }


        if (!(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("address must be InetSocketAddress");
        }
        String path = "/" + serviceName.replaceAll("\\.", "/");


        InetSocketAddress realAddress = (InetSocketAddress) address;
        String hostString = realAddress.getHostString();
        int port = realAddress.getPort();
        StringBuilder sb = new StringBuilder();
        sb.append(hostString).append(":").append(port);
        sb.append(":").append("0");

        if (writeZookeeperConfig(path, sb.toString(), type)) {
            LOGGER.debug("register service:[" + serviceName + "] to zookeeper");
        }
    }

    private ProviderInformation findProvider(SocketAddress address) {
        for (ProviderInformation provider : services) {
            if (provider.getAddress().equals(address)) {
                return provider;
            }
        }
        return null;
    }


    private boolean writeZookeeperConfig(String path, String insertData, String type) throws Exception {

        boolean isCreate = true;
        if (zkClient.checkExists().forPath(path) != null && zkClient.getChildren().forPath(path).size() > 0) {
            isCreate = false;
        }
        String nodePath = path + "/" + ZK_SERVICE_PREFIX;
        if (isCreate) {
            if (zkClient.checkExists().forPath(path) != null) {
                zkClient.delete().forPath(path);
            }
            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
                    .forPath(path, type.getBytes());
            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(nodePath, insertData.getBytes());
            return true;
        } else {
            String originalType = new String(zkClient.getData().forPath(path));
            if (originalType != type) {
                LOGGER.warn("The register load balance type [" + type + "] is not equal original type [" + originalType + "]");
            }

            List<String> childNames = zkClient.getChildren().forPath(path);
            for (String childName : childNames) {
                byte[] bytes = zkClient.getData().forPath(path + "/" + childName);
                String childData = new String(bytes);
                String originalAddress = childData.substring(0, childData.lastIndexOf(":"));
                String newAddress = insertData.substring(0, insertData.lastIndexOf(":"));
                if (originalAddress.equals(newAddress)) {
                    zkClient.delete().forPath(path + "/" + childName);
                }
            }

            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(nodePath, insertData.getBytes());
            return true;
        }
    }


    public static ServerBuild newBuilder() {
        return new ServerBuild();
    }


    private List<Engine> initEngines() {
        engines = new ArrayList<>();
        engines.add(new ProtostuffEngine());
        return engines;
    }

    public static class ServerBuild {

        private String host;
        private int port;
        private String zkConnectUrl;
        private int ioThreadSize;
        private String zkNamespace;
        private boolean useZip;
        private List<Engine> engines;


        private ServerBuild() {
            engines = new ArrayList<>();
        }


        public ServerBuild host(String host) {
            if (ANY_HOST.equals(host)) {
                throw new IllegalArgumentException("The host must not be 0.0.0.0");
            }
            this.host = host;
            return this;
        }

        public ServerBuild port(int port) {
            if (port <= 0 || port >= 65535) {
                throw new IllegalArgumentException("The port is not valid");
            }
            this.port = port;
            return this;
        }

        public ServerBuild zkConnect(String zkConnectUrl) {
            this.zkConnectUrl = zkConnectUrl;
            return this;
        }

        public ServerBuild zkNamespace(String zkNamespace) {
            this.zkNamespace = zkNamespace;
            return this;
        }

        public ServerBuild ioThreadSize(int size) {
            this.ioThreadSize = size;
            return this;
        }


        public ServerBuild zip(boolean useZip) {
            this.useZip = useZip;
            return this;
        }

        public ServerBuild addEngines(List<Engine> engines) {
            if (engines == null) {
                throw new IllegalArgumentException("The engines is null");
            }
            this.engines.addAll(engines);
            return this;
        }

        public ServerBuild addEngine(Engine engine) {
            if (engine == null) {
                throw new IllegalArgumentException("The engine is null");
            }
            this.engines.add(engine);
            return this;
        }


        public Server build() {
            DefaultServer server = new DefaultServer();
            if (Strings.isNullOrEmpty(host)) {
                throw new IllegalStateException("The host must be set");
            }
            server.host = host;
            if (port == 0) {
                port = ZRpcPropertiesConstant.DEFAULT_PORT;
            }
            server.port = port;
            /*if(Strings.isNullOrEmpty(zkConnectUrl)){
                throw new IllegalStateException("The zookeeper connect url must be set");
            }*/
            server.zkConnectUrl = zkConnectUrl;
            if (ioThreadSize == 0) {
                ioThreadSize = DEFAULT_IO_THREAD_SIZE;
            }
            server.ioThreadSize = ioThreadSize;
            if (Strings.isNullOrEmpty(zkNamespace)) {
                LOGGER.warn("The zookeeper namespace is empty");
            }
            server.zkNamespace = zkNamespace;
            server.useZip = useZip;
            server.engines = engines;
            server.init();
            return server;
        }
    }


}
