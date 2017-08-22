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

package com.zhuyiren.rpc.handler;


import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.exception.ExecuteException;
import com.zhuyiren.rpc.exception.NoConnectException;
import com.zhuyiren.rpc.exception.RpcServiceNotValidException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public class DefaultCallHandler implements CallHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCallHandler.class);

    private static final int STATE_SHOULD_CONNECTION = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_CLOSED = 2;
    private static final int STATE_CONNECTING = 3;


    private final AtomicLong callId;
    private final ConcurrentHashMap<Long, Call> callMap;
    private static final int TIME_OUT = 10000;
    private final Object synchronization;
    private volatile int state;
    private volatile SocketAddress remoteAddress;
    private final EventLoopGroup executors;
    private final Bootstrap bootstrap;
    private final ScheduledExecutorService connectThread;
    private final ClientHandlerInitializer clientHandlerInitializer;
    private volatile CallWriter callWriter;
    private final Map<String,Boolean> serviceState;


    public DefaultCallHandler(EventLoopGroup executors, ScheduledExecutorService connectThread, SocketAddress remoteAddress, boolean useZip) {
        callId = new AtomicLong(0);
        callMap = new ConcurrentHashMap<>();
        serviceState=new ConcurrentHashMap<>();
        clientHandlerInitializer = new ClientHandlerInitializer(this, useZip);
        synchronization = new Object();
        this.remoteAddress = remoteAddress;
        this.executors = executors;
        state = STATE_SHOULD_CONNECTION;
        this.connectThread = connectThread;
        bootstrap = new Bootstrap();
        configBootstrap();
    }

    private void configBootstrap() {
        bootstrap.group(DefaultCallHandler.this.executors)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIME_OUT)
                .handler(clientHandlerInitializer);
    }

    @Override
    public void call(Call call) {
        if (state != STATE_RUNNING) {
            call.setException(new NoConnectException("connect is not ready"));
            return;
        }
        if(!serviceState.get(call.getRequest().getServiceName())){
            call.setException(new RpcServiceNotValidException("service is not ready"));
            return;
        }
        long currentId = callId.getAndIncrement();
        Packet request = call.getRequest();
        request.setId(currentId);
        callMap.put(currentId, call);
        writeCall(call);
        while (!call.isDone()) {
            call.waitForComplete(TIME_OUT, TimeUnit.MILLISECONDS);
        }

    }

    @Override
    public void ready() {
        synchronized (synchronization) {
            state = STATE_RUNNING;
            synchronization.notifyAll();
        }
    }

    @Override
    public void connect(SocketAddress address) throws InterruptedException {
        remoteAddress=address;
        bootstrap.connect(remoteAddress).sync();
        synchronized (synchronization) {
            while (state != STATE_RUNNING && !Thread.interrupted()) {
                synchronization.wait();
            }
        }
    }

    @Override
    public void completeCall(Packet packet) {

        Call call = callMap.get(packet.getId());
        if (call == null) {
            return;
        }
        call.setResponse(packet);
        call.setException(packet.getException() == null ? null : new ExecuteException(packet.getException()));
        call.complete();
        callMap.remove(call.getRequest().getId());

    }

    @Override
    public void shutdown() {
        synchronized (synchronization) {
            state = STATE_CLOSED;
        }
        callWriter.close();
    }

    @Override
    public void close() {
        synchronized (synchronization) {
            if (state == STATE_RUNNING) {
                state = STATE_CONNECTING;
                Map<Long, Call> oldCalls = new HashMap<>(callMap);

                connectThread.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            connect(remoteAddress);
                            for (Map.Entry<Long, Call> entry : oldCalls.entrySet()) {
                                writeCall(entry.getValue());
                            }
                        } catch (Exception e) {
                            connectThread.submit(this);
                        }
                    }
                });
            }
        }
    }


    @Override
    public void writeCall(Call call) {
        callWriter.writeRequestPacket(call.getRequest());
    }

    @Override
    public void setCallWriter(CallWriter callWriter) {
        this.callWriter = callWriter;
    }

    @Override
    public boolean setServiceState(String serviceName, boolean state) {
        serviceState.put(serviceName, state);
        Boolean removed=true;
        for (Map.Entry<String, Boolean> entry : serviceState.entrySet()) {
            if(entry.getValue()==true){
                removed=false;
                break;
            }
        }
        return removed;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultCallHandler)) return false;
        DefaultCallHandler that = (DefaultCallHandler) o;
        return Objects.equals(remoteAddress, that.remoteAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteAddress);
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }



}
