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
import com.zhuyiren.rpc.handler.CallHandler;
import com.zhuyiren.rpc.handler.DefaultCallHandler;
import com.zhuyiren.rpc.handler.DefaultInvoker;
import com.zhuyiren.rpc.handler.Invoker;
import com.zhuyiren.rpc.loadbalance.LoadBalanceStrategy;
import com.zhuyiren.rpc.loadbalance.RandomLoadBalanceStrategy;
import com.zhuyiren.rpc.utils.CommonUtils;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
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

    private static final int DEFAULT_IO_THREAD_SIZE = Runtime.getRuntime().availableProcessors()*2;

    private final List<Engine> engines;
    private final Map<SocketAddress, CallHandler> callerMap;
    private final Map<String, ServiceInformation> serviceInfoMaps;
    private final Map<String, Invoker> invokerMap;
    private final Map<String, Class<? extends LoadBalanceStrategy>> loadBalanceStrategyMap;

    private volatile NioEventLoopGroup eventExecutors;
    private final ScheduledExecutorService connectThread;
    private volatile CuratorFramework zkClient;
    private final boolean useZip;


    public void setEventExecutors(NioEventLoopGroup eventExecutors){
        this.eventExecutors=eventExecutors;
    }

    public NioEventLoopGroup getEventExecutors(){
        return eventExecutors;
    }


    private PathChildrenCacheListener childWatcher = new PathChildrenCacheListener() {
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
                List<ProviderProperty> providerProperties = extractProviderAddress(serviceName);
                List<SocketAddress> newAddresses = providerProperties.stream().map(key -> key.getAddress()).collect(Collectors.toList());

                List<SocketAddress> removeAddresses = new ArrayList<>(originalAddresses);
                List<SocketAddress> addAddresses = new ArrayList<>(newAddresses);
                List<SocketAddress> intersectionAddresses = new ArrayList<>(originalAddresses);

                removeAddresses.removeAll(newAddresses);
                addAddresses.removeAll(originalAddresses);
                intersectionAddresses.retainAll(newAddresses);

                for (SocketAddress removeAddress : removeAddresses) {
                    CallHandler handler = callerMap.get(removeAddress);
                    if (handler != null) {
                        if (handler.setServiceState(serviceName, false)) {
                            removeCaller(handler);
                        }
                    }
                }

                List<SocketAddress> reallyAddresses = createCallWithService(serviceName, addAddresses);
                reallyAddresses.addAll(intersectionAddresses);
                List<ProviderProperty> configs = providerProperties.stream().filter(provider ->
                        reallyAddresses.contains(provider.getAddress())
                ).collect(Collectors.toList());
                serviceInfoMaps.put(serviceName, new ServiceInformation(serviceName, originalServiceInfo.getLoadBalanceType(), true, configs));
                invokerMap.get(serviceName).getLoadBalanceStrategy().init(serviceName);
            }
        }
    };

    private class LoadBalanceWatcher implements NodeCacheListener {

        private final String serviceName;
        private final String path;

        public LoadBalanceWatcher(String serviceName) {
            this.serviceName = serviceName;
            path = "/" + serviceName.replaceAll("\\.", "/");
        }


        @Override
        public void nodeChanged() throws Exception {
            synchronized (DefaultClient.this) {
                if (zkClient.checkExists().forPath(path) == null) {
                    return;
                }

                String loadBalance = getLoadBalanceTypeFromCoordination(serviceName);
                LoadBalanceStrategy strategy = invokerMap.get(serviceName).getLoadBalanceStrategy();
                if (loadBalance.equals(strategy.getType())) {
                    return;
                }

                LoadBalanceStrategy newStrategy = getLoadBalanceStrategy(loadBalance);
                if (newStrategy == null) {
                    LOGGER.warn("The load balance type [" + loadBalance + "] is not valid");
                    return;
                }

                newStrategy.init(serviceName);
                invokerMap.get(serviceName).setLoadBalanceStrategy(newStrategy);


            }
        }
    }

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
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service, List<ProviderProperty> providers) throws Exception {
        return exportService(engineType, service, service.getCanonicalName(), providers);
    }

    @Override
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service, String serviceName, List<ProviderProperty> providers) throws Exception {

        if (providers != null && providers.size() > 0) {
            List<SocketAddress> collectAddresses = providers.stream().map(key -> key.getAddress()).collect(Collectors.toList());
            CommonUtils.checkNoAnyHost(collectAddresses);
        }

        Engine engine = findEngineByType(engineType);
        if (engine == null) {
            engine = addEngineByClass(engineType);
        }


        String loadBalanceType;
        //采用zookeeper管理服务
        boolean isZkManage = false;
        if (providers == null || providers.isEmpty()) {
            isZkManage = true;
            providers = extractProviderAddress(serviceName);
            loadBalanceType = getLoadBalanceTypeFromCoordination(serviceName);
        } else {
            loadBalanceType = checkUniqueLoadBalanceType(providers);
        }

        if (Strings.isNullOrEmpty(loadBalanceType)) {
            loadBalanceType = RandomLoadBalanceStrategy.LOAD_BALANCE_TYPE;
        }

        LoadBalanceStrategy strategy = getLoadBalanceStrategy(loadBalanceType);
        if (strategy == null) {
            LOGGER.warn("Can't find the load balance strategy,and will use RANDOM strategy");
            strategy = new RandomLoadBalanceStrategy(this, this);
        }
        LOGGER.debug("The service [" + serviceName + "] using load balance strategy [" + loadBalanceType + "]");


        List<SocketAddress> connectedAddresses = createCallWithService(serviceName, providers.stream().map(key -> key.getAddress()).collect(Collectors.toList()));
        List<ProviderProperty> providerConfigs = providers.stream().filter((item) -> connectedAddresses.contains(item.getAddress())).collect(Collectors.toList());
        ServiceInformation serviceInfo = new ServiceInformation(serviceName, loadBalanceType, isZkManage, providerConfigs);
        serviceInfoMaps.put(serviceName, serviceInfo);

        Invoker invoker = new DefaultInvoker(serviceName, engine, strategy);
        strategy.init(serviceName);
        invokerMap.put(serviceName, invoker);
        Object o = Proxy.newProxyInstance(Engine.class.getClassLoader(), new Class[]{service}, invoker);

        if (serviceInfo.isManageByZookeeper()) {
            watchService(serviceName);
        }
        return (T) o;
    }

    private String checkUniqueLoadBalanceType(List<ProviderProperty> configs) {
        List<ProviderProperty> unique = new ArrayList<>();
        for (ProviderProperty config : configs) {
            if (Strings.isNullOrEmpty(config.getLoadBalanceType())) {
                continue;
            }
            boolean isContain = false;

            for (ProviderProperty uniqueConfig : unique) {
                if(config.getLoadBalanceType().equals(uniqueConfig.getLoadBalanceType())){
                    isContain=true;
                    break;
                }
            }
            if (!isContain) {
                unique.add(config);
            }
        }
        if (unique.size() != 1) {
            LOGGER.warn("The load balance type of [" + configs + "] is not identical.");
            return null;
        }
        return unique.get(0).getLoadBalanceType();


    }

    private List<SocketAddress> createCallWithService(String serviceName, List<SocketAddress> addresses) {
        List<SocketAddress> really = new ArrayList<>();
        for (SocketAddress address : addresses) {
            CallHandler callHandler = callerMap.get(address);
            if (callHandler == null) {
                try {
                    callHandler = createCaller(address);
                } catch (Exception e) {
                    LOGGER.warn("Can't connect to [" + address + "]");
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
    public LoadBalanceStrategy getLoadBalanceStrategy(String type) {
        Class<? extends LoadBalanceStrategy> clz = loadBalanceStrategyMap.get(type);
        if (clz == null) {
            return null;
        }
        try {
            Constructor<? extends LoadBalanceStrategy> constructor = clz.getDeclaredConstructor(ServiceManager.class, CallHandlerManager.class);
            return constructor.newInstance(this, this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error("Instantiation class [" + clz + "] occur error", e);
            return null;
        }
    }

    @Override
    public void registerLoadBalance(Class<? extends LoadBalanceStrategy> strategy) {

        try {
            Field typeField = strategy.getDeclaredField("LOAD_BALANCE_TYPE");
            String type = (String) typeField.get(null);
            loadBalanceStrategyMap.put(type, strategy);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Get load balance literal occur error", e);
        }

    }

    @Override
    public boolean watchService(String serviceName) {
        String path = "/" + serviceName.replaceAll("\\.", "/");
        PathChildrenCache childrenCache = new PathChildrenCache(zkClient, path, false);
        childrenCache.getListenable().addListener(childWatcher);
        NodeCache currentNode = new NodeCache(zkClient, path, false);
        currentNode.getListenable().addListener(new LoadBalanceWatcher(serviceName));
        try {
            childrenCache.start();
            currentNode.start();
            return true;
        } catch (Exception e) {
            LOGGER.error("Watch service [" + serviceName + "] occur error", e);
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
    public List<ProviderProperty> getProviderLoadBalanceConfigs(String serviceName) {
        return serviceInfoMaps.get(serviceName).getProviderProperties();
    }


    private String getLoadBalanceTypeFromCoordination(String serviceName) {
        String loadBalance;
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            LOGGER.warn("The zookeeper is not connecting,The load balance will use RANDOM type");
            loadBalance = null;
        } else {
            String path = "/" + serviceName.replaceAll("\\.", "/");
            try {
                loadBalance = new String(zkClient.getData().forPath(path), StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.warn("Extract load balance type from zookeeper occur error,The load balance will use RANDOM type");
                loadBalance = null;
            }
        }
        return loadBalance;
    }

    @Override
    public List<ProviderProperty> extractProviderAddress(String serviceName) throws Exception {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw new IllegalStateException("The [" + serviceName + "] is not configuration provider address,and the client is not connecting zookeeper");
        }
        String path = "/" + serviceName.replaceAll("\\.", "/");

        List<String> childNames = zkClient.getChildren().forPath(path);

        return generateOptimal(path, childNames);

    }

    private List<ProviderProperty> generateOptimal(String parentPath, List<String> childNames) throws Exception {
        List<String> configs = new ArrayList<>();
        for (String childName : childNames) {
            String s = new String(zkClient.getData().forPath(parentPath + "/" + childName), StandardCharsets.UTF_8);
            configs.add(s);
        }

        return configs.stream().map(s -> {
            int hostIndex = s.indexOf(':');
            String host = s.substring(0, hostIndex);
            int portIndex = s.indexOf(':', hostIndex + 1);
            int port = Integer.parseInt(s.substring(hostIndex + 1, portIndex));
            InetSocketAddress address = new InetSocketAddress(host, port);
            return new ProviderProperty(address, null, s.substring(portIndex + 1));
        }).distinct().collect(Collectors.toList());
    }


    private String getServiceByPath(String fullPath) {
        String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/'));
        return parentPath.substring(1).replaceAll("/", "\\.");
    }
}
