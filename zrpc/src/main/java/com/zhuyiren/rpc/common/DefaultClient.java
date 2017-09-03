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
import com.zhuyiren.rpc.handler.*;
import com.zhuyiren.rpc.utils.CommonUtils;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public class DefaultClient implements Client, ServiceManager, CallHandlerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClient.class);

    private static final int DEFAULT_IO_THREAD_SIZE = Runtime.getRuntime().availableProcessors();

    private List<Engine> engines;
    private Map<SocketAddress, CallHandler> callerMap;
    private Map<String, ServiceInformation> serviceInfoMaps;
    private Map<String, Invoker> invokerMap;
    private Map<LoadBalanceType, LoadBalanceStrategy> loadBalanceStrategyMap;

    private NioEventLoopGroup eventExecutors;
    private ScheduledExecutorService connectThread;
    private CuratorFramework zkClient;
    private boolean useZip;


    private PathChildrenCacheListener watcher = new PathChildrenCacheListener() {
        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            synchronized (DefaultClient.this) {
                if (pathChildrenCacheEvent.getData() == null) {
                    return;
                }
                String childPath = pathChildrenCacheEvent.getData().getPath();
                String serviceName = getServiceByPath(childPath);


                ServiceInformation originalServiceInfo = serviceInfoMaps.get(serviceName);
                if (originalServiceInfo == null || !originalServiceInfo.isManageByZookeeper()) {
                    return;
                }
                List<SocketAddress> originalAddresses = originalServiceInfo.getAddresses();
                List<SocketAddress> newAddresses = extractProviderAddress(serviceName);

                List<SocketAddress> removeAddresses = new ArrayList<>(originalAddresses);
                List<SocketAddress> addAddresses = new ArrayList<>(newAddresses);

                removeAddresses.removeAll(newAddresses);
                addAddresses.removeAll(originalAddresses);

                for (SocketAddress removeAddress : removeAddresses) {
                    CallHandler handler = callerMap.get(removeAddress);
                    if (handler != null) {
                        if (handler.setServiceState(serviceName, false)) {
                            removeCaller(handler);
                        }
                    }
                }

                createCallWithService(serviceName, addAddresses);

            }
        }
    };

    public DefaultClient() {
        this(DEFAULT_IO_THREAD_SIZE);
    }

    public DefaultClient(int ioThreadSize) {
        this(ioThreadSize, false);
    }

    public DefaultClient(int ioThreadSize, boolean useZip) {
        this(null, null, ioThreadSize, useZip);
    }

    public DefaultClient(String zkConnectUrl, String zkNamespace, int threadSize) {
        this(zkConnectUrl, zkNamespace, threadSize, false);
    }

    public DefaultClient(String zkConnectUrl, String zkNamespace, int ioThreadSize, boolean useZip) {
        engines = new ArrayList<>();
        callerMap = new ConcurrentHashMap<>();
        serviceInfoMaps = new ConcurrentHashMap<>();
        invokerMap = new ConcurrentHashMap<>();
        loadBalanceStrategyMap = new ConcurrentHashMap<>();
        eventExecutors = ioThreadSize == 0 ? new NioEventLoopGroup() : new NioEventLoopGroup(ioThreadSize);
        LOGGER.debug("io thread size:" + eventExecutors.executorCount());
        connectThread = Executors.newScheduledThreadPool(1);
        this.useZip = useZip;
        if (!Strings.isNullOrEmpty(zkConnectUrl)) {
            zkClient = CuratorFrameworkFactory.builder()
                    .connectString(zkConnectUrl).retryPolicy(new RetryNTimes(10, 5000))
                    .namespace(zkNamespace).build();
            zkClient.start();
        }
    }


    @Override
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service, List<SocketAddress> addresses) throws Exception {
        return exportService(engineType, service, service.getCanonicalName(), addresses);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service, String serviceName, List<SocketAddress> addresses) throws Exception {

        if (addresses != null && addresses.size() > 0) {
            CommonUtils.checkNoAnyHost(addresses);
        }

        Engine engine = findEngineByType(engineType);
        if (engine == null) {
            engine = addEngineByClass(engineType);
        }

        //采用zookeeper管理服务
        boolean isZkManage = false;
        if (addresses == null || addresses.size() == 0) {
            isZkManage = true;
            addresses = extractProviderAddress(serviceName);
        }


        LoadBalanceType loadBalanceType = getLoadBalanceType(serviceName);

        LoadBalanceStrategy loadBalanceStrategy = loadBalanceStrategyMap.get(loadBalanceType);
        if (loadBalanceStrategy == null) {
            LOGGER.warn("Can't find the load balance strategy for [" + loadBalanceType + "],and will use RANDOM strategy");
            loadBalanceStrategy = new RandomLoadBalanceStrategy(this, this);
        }
        Invoker invoker = new DefaultInvoker(serviceName, engine, loadBalanceStrategy);


        List<SocketAddress> connectedAddresses=createCallWithService(serviceName, addresses);

        ServiceInformation serviceInfo = new ServiceInformation(serviceName, loadBalanceType, isZkManage, connectedAddresses);
        serviceInfoMaps.put(serviceName, serviceInfo);
        invokerMap.put(serviceName, invoker);
        Object o = Proxy.newProxyInstance(Engine.class.getClassLoader(), new Class[]{service}, invoker);

        if (serviceInfo.isManageByZookeeper()) {
            watchService(serviceName);
        }
        return (T) o;
    }


    private List<SocketAddress> createCallWithService(String serviceName, List<SocketAddress> addresses){
        List<SocketAddress> really=new ArrayList<>();
        for (SocketAddress address : addresses) {
            CallHandler callHandler = callerMap.get(address);
            if (callHandler == null) {
                try {
                    callHandler = createCaller(address);
                } catch (Exception e) {
                    LOGGER.warn("Can't connect to ["+address+"]");
                    continue;
                }
            }
            really.add(address);
            callHandler.setServiceState(serviceName, true);
        }
        return really;
    }

    private Engine findEngineByType(Class<? extends Engine> engineType) {
        for (Engine engine : engines) {
            if (engine.getClass().equals(engineType)) {
                return engine;
            }
        }
        return null;
    }

    @Override
    public CallHandler createCaller(SocketAddress address) throws Exception {
        CallHandler callHandler = new DefaultCallHandler(eventExecutors, connectThread, address, useZip);
        callHandler.connect(address);
        callerMap.putIfAbsent(address, callHandler);
        return callHandler;
    }

    @Override
    public CallHandler removeCaller(CallHandler callHandler) {
        SocketAddress remoteAddress = callHandler.getRemoteAddress();
        callerMap.remove(remoteAddress);
        callHandler.shutdown();
        return callHandler;
    }

    @Override
    public CallHandler getCallHandler(SocketAddress address) {
        return callerMap.get(address);
    }

    /**
     * 关闭所有资源
     */
    @Override
    public void shutdown() {
        for (Map.Entry<SocketAddress, CallHandler> entry : callerMap.entrySet()) {
            entry.getValue().shutdown();
        }
        eventExecutors.shutdownGracefully();
        connectThread.shutdownNow();
    }


    @Override
    public Engine addEngineByClass(Class<? extends Engine> engineClass) throws Exception {
        Engine engine = engineClass.newInstance();
        return addEngine(engine);

    }


    @Override
    public Engine addEngine(Engine engine) {
        if (!engines.contains(engine)) {
            engines.add(engine);
        }
        return engine;
    }


    @Override
    public LoadBalanceStrategy getLoadBalanceStrategy(LoadBalanceType type) {
        return loadBalanceStrategyMap.get(type);
    }

    @Override
    public void registerLoadBalance(LoadBalanceType type, LoadBalanceStrategy strategy) {
        loadBalanceStrategyMap.put(type, strategy);
    }

    @Override
    public boolean watchService(String serviceName) {
        String path = "/" + serviceName.replaceAll("\\.", "/");
        PathChildrenCache cache = new PathChildrenCache(zkClient, path, true);
        cache.getListenable().addListener(watcher);
        try {
            cache.start();
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<SocketAddress> getServiceProviderAddress(String serviceName) {
        ServiceInformation information = serviceInfoMaps.get(serviceName);
        if (information == null) {
            return new ArrayList<>();
        }
        return information.getAddresses();
    }

    @Override
    public LoadBalanceType getLoadBalanceType(String serviceName) {

        ServiceInformation originalInformation = serviceInfoMaps.get(serviceName);
        LoadBalanceType loadBalance = null;
        if (originalInformation != null) {
            loadBalance = originalInformation.getLoadBalanceType();
        } else {
            if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
                LOGGER.warn("The zookeeper is not connecting,The load balance will use RANDOM type");
                loadBalance = LoadBalanceType.RANDOM;
            } else {
                String path = "/" + serviceName.replaceAll("\\.", "/");
                String loadTypeString = null;
                try {
                    loadTypeString = new String(zkClient.getData().forPath(path));
                    loadBalance = LoadBalanceType.of(loadTypeString);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("The register load balance type [" + loadTypeString + "] is not valid,The load balance will use RANDOM type");
                    loadBalance = loadBalance.RANDOM;
                } catch (Exception e) {
                    LOGGER.warn("Extract load balance type from zookeeper occur error,The load balance will use RANDOM type");
                    loadBalance = LoadBalanceType.RANDOM;
                }
            }
        }
        return loadBalance;

    }

    @Override
    public List<SocketAddress> extractProviderAddress(String serviceName) throws Exception {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw new IllegalStateException("The [" + serviceName + "] is not configuration provider address,and the client is not connecting zookeeper");
        }
        String path = "/" + serviceName.replaceAll("\\.", "/");

        List<String> childNames = zkClient.getChildren().forPath(path);

        return generateOptimal(path, childNames);

    }

    private List<SocketAddress> generateOptimal(String parentPath, List<String> childNames) throws Exception {
        List<String> configs = new ArrayList<>();
        for (String childName : childNames) {
            String s = new String(zkClient.getData().forPath(parentPath + "/" + childName));
            configs.add(s);
        }

        return configs.stream().map(s -> {
            int hostIndex = s.indexOf(":");
            String host = s.substring(0, hostIndex);
            int portIndex = s.indexOf(":", hostIndex + 1);
            int port = Integer.parseInt(s.substring(hostIndex + 1, portIndex));
            return new InetSocketAddress(host, port);
        }).distinct().collect(Collectors.toList());
    }


    private String getServiceByPath(String fullPath) {
        String parentPath = fullPath.substring(0, fullPath.lastIndexOf("/"));
        return parentPath.substring(1).replaceAll("/", "\\.");
    }
}
