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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public class DefaultClient implements Client, ZkRegister, CallHandlerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClient.class);

    private List<Engine> engines;
    private Map<SocketAddress, CallHandler> callerMap;
    private List<CallHandler> callHandlers;
    private Map<String, CallHandler> zkServiceMap;
    private Map<String, Invoker> invokerMap;

    private NioEventLoopGroup eventExecutors;
    private ScheduledExecutorService connectThread;
    private CuratorFramework zkClient;
    private String zkConnectUrl;
    private boolean useZip;


    private PathChildrenCacheListener watcher=new PathChildrenCacheListener() {
        @Override
        public synchronized void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            if(pathChildrenCacheEvent.getData()==null){
                return;
            }

            String childPath = pathChildrenCacheEvent.getData().getPath();
            String path=childPath.substring(0,childPath.lastIndexOf("/"));
            String serviceName = path.substring(1).replaceAll("/", "\\.");
            try {
                CallHandler originalCaller = zkServiceMap.get(serviceName);
                if (originalCaller == null) {
                    return;
                }
                SocketAddress originalAddress=originalCaller.getRemoteAddress();
                SocketAddress newAddress= getChangedAddress(serviceName);

                LOGGER.debug("The service:[" + serviceName + "] is changed,the new address:[" + newAddress + "]");

                if(originalAddress.equals(newAddress)){
                    return;
                }

                CallHandler findCaller = null;
                if (newAddress == null || (findCaller = callerMap.get(newAddress)) != null) {
                    boolean shouldRemove = originalCaller.setServiceState(serviceName, false);
                    if (shouldRemove) {
                        removeCaller(originalCaller);
                    }
                    if (findCaller != null) {
                        invokerMap.get(serviceName).setCallHandler(findCaller);
                        findCaller.setServiceState(serviceName, true);
                    }
                    return;
                }
                CallHandler newCaller = createCaller(newAddress);
                newCaller.setServiceState(serviceName,true);
                invokerMap.get(serviceName).setCallHandler(newCaller);
                zkServiceMap.put(serviceName,newCaller);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    };


    public DefaultClient(String zkConnectUrl, int threadSize) {
        this(zkConnectUrl, threadSize, false);
    }

    public DefaultClient(String zkConnectUrl, int threadSize, boolean useZip) {
        engines = new ArrayList<>();
        callerMap = new ConcurrentHashMap<>();
        zkServiceMap = new ConcurrentHashMap<>();
        callHandlers = new CopyOnWriteArrayList<>();
        invokerMap = new ConcurrentHashMap<>();
        eventExecutors = threadSize == 0 ? new NioEventLoopGroup() : new NioEventLoopGroup(threadSize);
        LOGGER.debug("io thread size:" + eventExecutors.executorCount());
        connectThread = Executors.newScheduledThreadPool(1);
        this.useZip = useZip;
        this.zkConnectUrl = zkConnectUrl;
        zkClient = CuratorFrameworkFactory.newClient(this.zkConnectUrl, new RetryNTimes(10, 5000));
        zkClient.start();
    }


    @Override
    public <T> T exportService(Class<? extends Engine> engineType, Class<T> service, SocketAddress address, boolean useCache) throws Exception {
        return exportService(engineType, service, service.getCanonicalName(), address, useCache);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service, String serviceName, SocketAddress address, boolean useCache) throws Exception {
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
            watchService(serviceName);
        }

        CallHandler callHandler = null;
        if (useCache) {
            callHandler = callerMap.get(address);
        }
        if (callHandler == null) {
            callHandler = createCaller(address);
            callHandler.setServiceState(serviceName, true);
        }
        invoker.setCallHandler(callHandler);
        if (isZkManage) {
            zkServiceMap.put(serviceName, callHandler);
        }
        invokerMap.put(serviceName, invoker);
        Object o = Proxy.newProxyInstance(Engine.class.getClassLoader(), new Class[]{service}, invoker);
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
        callHandlers.add(callHandler);
        return callHandler;
    }

    @Override
    public CallHandler removeCaller(CallHandler callHandler) {
        SocketAddress remoteAddress = callHandler.getRemoteAddress();
        callerMap.remove(remoteAddress);
        callHandlers.remove(callHandler);
        callHandler.shutdown();

        return callHandler;

    }

    /**
     * 关闭所有资源
     */
    @Override
    public void shutdown() {
        for (CallHandler callHandler : callHandlers) {
            callHandler.shutdown();
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
        String path="/"+serviceName.replaceAll("\\.","/");
        PathChildrenCache cache = new PathChildrenCache(zkClient, path, true);
        cache.getListenable().addListener(watcher );
        try {
            cache.start();
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
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
}
