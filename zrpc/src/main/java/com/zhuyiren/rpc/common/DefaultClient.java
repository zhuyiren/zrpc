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

import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.handler.CallHandler;
import com.zhuyiren.rpc.handler.DefaultCallHandler;
import com.zhuyiren.rpc.handler.DefaultInvoker;
import com.zhuyiren.rpc.handler.Invoker;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.zhuyiren.rpc.common.CommonConstant.ANY_HOST;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public class DefaultClient implements Client, ServiceManage, CallHandlerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClient.class);

    private List<Engine> engines;
    private Map<SocketAddress, CallHandler> callerMap;
    private Map<String, ServiceInformation> serviceInfoMaps;
    private Map<String, Invoker> invokerMap;

    private NioEventLoopGroup eventExecutors;
    private ScheduledExecutorService connectThread;
    private CuratorFramework zkClient;
    private String zkConnectUrl;
    private boolean useZip;
    private String zkNamespace;


    private PathChildrenCacheListener watcher = new PathChildrenCacheListener() {
        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            synchronized (DefaultClient.this){
                if (pathChildrenCacheEvent.getData() == null) {
                    return;
                }
                String childPath = pathChildrenCacheEvent.getData().getPath();
                String serviceName = getServiceByPath(childPath);

                try {
                    ServiceInformation serviceInfo = serviceInfoMaps.get(serviceName);
                    if (serviceInfo==null || !serviceInfo.isManageByZookeeper) {
                        return;
                    }
                    SocketAddress oldAddress = serviceInfo.address;
                    SocketAddress newAddress = getChangedAddress(serviceName);

                    LOGGER.debug("The service:[" + serviceName + "] is changed,the new address:[" + newAddress + "]");

                    CallHandler oldCaller;
                    CallHandler newCaller;

                    if(newAddress==null){
                        oldCaller=callerMap.get(oldAddress);
                        if(oldCaller!=null){
                            if (oldCaller.setServiceState(serviceName,false)) {
                                removeCaller(oldCaller);
                            }
                        }
                        serviceInfoMaps.put(serviceName,new ServiceInformation(serviceName,true,newAddress));
                        return;
                    }
                    newCaller=callerMap.get(newAddress);
                    oldCaller=callerMap.get(oldAddress);
                    if(newCaller==null){
                        newCaller=createCaller(newAddress);
                    }
                    newCaller.setServiceState(serviceName,true);
                    serviceInfoMaps.put(serviceName,new ServiceInformation(serviceName,true,newAddress));
                    invokerMap.get(serviceName).setCallHandler(newCaller);
                    if(!newCaller.equals(oldCaller)){
                        if (oldCaller.setServiceState(serviceName,false)) {
                            removeCaller(oldCaller);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

    };


    public DefaultClient(String zkConnectUrl, String zkNamespace, int threadSize) {
        this(zkConnectUrl, zkNamespace, threadSize, false);
    }

    public DefaultClient(String zkConnectUrl, String zkNamespace, int threadSize, boolean useZip) {
        engines = new ArrayList<>();
        callerMap = new ConcurrentHashMap<>();
        serviceInfoMaps=new ConcurrentHashMap<>();
        invokerMap = new ConcurrentHashMap<>();
        eventExecutors = threadSize == 0 ? new NioEventLoopGroup() : new NioEventLoopGroup(threadSize);
        LOGGER.debug("io thread size:" + eventExecutors.executorCount());
        connectThread = Executors.newScheduledThreadPool(1);
        this.useZip = useZip;
        this.zkConnectUrl = zkConnectUrl;
        this.zkNamespace = zkNamespace;
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(this.zkConnectUrl).retryPolicy(new RetryNTimes(10, 5000))
                .namespace(this.zkNamespace).build();
        zkClient.start();
    }


    @Override
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service, SocketAddress address ) throws Exception {
        return exportService(engineType, service, service.getCanonicalName(), address);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service, String serviceName, SocketAddress address ) throws Exception {

        if (address != null && address instanceof InetSocketAddress) {
            if (ANY_HOST.equals(((InetSocketAddress) address).getAddress().getHostAddress())) {
                throw new IllegalArgumentException("The address must not be 0.0.0.0");
            }
        }
        Engine engine = findEngineByType(engineType);
        if (engine == null) {
            engine = addEngineByClass(engineType);
        }
        Invoker invoker = new DefaultInvoker(serviceName);
        invoker.setEngine(engine);

        //采用zookeeper管理服务
        boolean isZkManage = false;
        if (address == null) {
            isZkManage = true;
            address = getChangedAddress(serviceName);
        }
        ServiceInformation serviceInfo = new ServiceInformation(serviceName, isZkManage, address);
        serviceInfoMaps.put(serviceName, serviceInfo);


        CallHandler callHandler=callerMap.get(address);
        if (callHandler == null) {
            callHandler = createCaller(address);
        }
        callHandler.setServiceState(serviceName, true);
        invoker.setCallHandler(callHandler);
        invokerMap.put(serviceName, invoker);
        Object o = Proxy.newProxyInstance(Engine.class.getClassLoader(), new Class[]{service}, invoker);

        if(serviceInfo.isManageByZookeeper){
            watchService(serviceName);
        }
        return (T) o;
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
    public SocketAddress getChangedAddress(String serviceName) throws Exception {
        String path = "/" + serviceName.replaceAll("\\.", "/");
        List<String> childNames = zkClient.getChildren().forPath(path);
        if (childNames == null || childNames.size() == 0) {
            return null;
        }
        return generateOptimal(path, childNames);
    }

    private SocketAddress generateOptimal(String parentPath, List<String> childNames) throws Exception {
        List<String> configs = new ArrayList<>();
        for (String childName : childNames) {
            String s = new String(zkClient.getData().forPath(parentPath + "/" + childName));
            configs.add(s);
        }
        String targetConfig = Collections.max(configs, (o1, o2) -> {
            int temp1 = Integer.parseInt(o1.substring(o1.lastIndexOf(":") + 1));
            int temp2 = Integer.parseInt(o2.substring(o2.lastIndexOf(":") + 1));
            return temp1 - temp2;
        });
        int hostIndex = targetConfig.indexOf(":");
        String host = targetConfig.substring(0, hostIndex);
        int portIndex = targetConfig.indexOf(":", hostIndex + 1);
        int port = Integer.parseInt(targetConfig.substring(hostIndex + 1, portIndex));
        return new InetSocketAddress(host, port);
    }


    private String getServiceByPath(String fullPath){
        String parentPath = fullPath.substring(0, fullPath.lastIndexOf("/"));
        String serviceName = parentPath.substring(1).replaceAll("/", "\\.");
        return serviceName;
    }

    private final static class ServiceInformation {
        private final String serviceName;
        private final boolean isManageByZookeeper;
        private final SocketAddress address;

        public ServiceInformation(String serviceName, boolean isManageByZookeeper, SocketAddress address) {
            this.serviceName = serviceName;
            this.isManageByZookeeper = isManageByZookeeper;
            this.address = address;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ServiceInformation)) return false;
            ServiceInformation that = (ServiceInformation) o;
            return Objects.equals(serviceName, that.serviceName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceName);
        }
    }
}
