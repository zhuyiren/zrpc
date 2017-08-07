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
import com.zhuyiren.rpc.handler.CallHandler;
import com.zhuyiren.rpc.handler.DefaultCallHandler;
import com.zhuyiren.rpc.handler.DefaultInvoker;
import com.zhuyiren.rpc.handler.Invoker;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public class DefaultClient implements Client {

    private static final Logger LOGGER= LoggerFactory.getLogger(DefaultClient.class);


    List<Engine> engines;
    Map<SocketAddress, CallHandler> callerMap;
    List<CallHandler> callHandlers;
    NioEventLoopGroup eventExecutors;
    ScheduledExecutorService connectThread;


    public DefaultClient() {
        this(0);
    }

    public DefaultClient(int threadSize) {
        engines = new ArrayList<>();
        callerMap = new HashMap<>();
        callHandlers = new ArrayList<>();
        eventExecutors = threadSize == 0 ? new NioEventLoopGroup() : new NioEventLoopGroup(threadSize);
        LOGGER.debug("io thread size:" + eventExecutors.executorCount());
        connectThread = Executors.newScheduledThreadPool(1);
    }


    @Override
    public <T> T exportService(Class<? extends Engine> engineType, Class<T> service, SocketAddress address, boolean useCache) throws Exception {
        return exportService(engineType,service,service.getCanonicalName(),address,useCache);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T exportService(Class<? extends Engine> engineType, Class<T> service,String serviceName, SocketAddress address, boolean useCache) throws Exception {
        Engine engine = findEngineByType(engineType);
        if (engine == null) {
            throw new IllegalArgumentException("illegal engine");
        }
        Invoker invoker = new DefaultInvoker(serviceName);
        invoker.setEngine(engine);

        CallHandler callHandler = null;
        if (useCache) {
            callHandler = callerMap.get(address);
        }
        if (callHandler == null) {
            callHandler = new DefaultCallHandler(eventExecutors, connectThread, address);
            callHandler.connect();
            callerMap.putIfAbsent(address, callHandler);
            callHandlers.add(callHandler);
        }
        invoker.setCallHandler(callHandler);
        Object o = Proxy.newProxyInstance(Engine.class.getClassLoader(), new Class[]{service}, invoker);


        return (T) o;
    }

    @Override
    public void registerEngine(Engine engine) {
        if (!engines.contains(engine)) {
            engines.add(engine);
        }
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
    public void shutdown() {
        for (CallHandler callHandler : callHandlers) {
            callHandler.shutdown();
        }
        eventExecutors.shutdownGracefully();
        connectThread.shutdownNow();
    }
}
