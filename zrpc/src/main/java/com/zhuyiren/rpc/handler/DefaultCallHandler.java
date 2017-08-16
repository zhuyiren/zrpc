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
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public class DefaultCallHandler implements CallHandler {

    private static final Logger LOGGER= LoggerFactory.getLogger(DefaultCallHandler.class);

    private static final int STATE_SHOULD_CONNECTION = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_CLOSED = 2;
    private static final int STATE_CONNECTING = 3;

    private static final int CALL_TIME_OUT = 5000;


    private final AtomicLong callId;
    private final ConcurrentHashMap<Long, Call> calls;
    private static final int TIME_OUT = 10000;
    private final Object synchronization;
    private volatile int state;
    private SocketAddress remoteAddress;
    private EventLoopGroup executors;
    private Bootstrap bootstrap;
    private ScheduledExecutorService connectThread;
    private ClientHandlerInitializer clientHandlerInitializer;
    private CallWriter callWriter;


    public DefaultCallHandler(EventLoopGroup executors, ScheduledExecutorService connectThread, SocketAddress remoteAddress,boolean useZip) {
        callId = new AtomicLong(0);
        calls = new ConcurrentHashMap<>();
        clientHandlerInitializer = new ClientHandlerInitializer(this,useZip);
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
        long currentId = callId.getAndIncrement();
        Packet request = call.getRequest();
        request.setId(currentId);
        calls.put(currentId, call);
        writeCall(call);
        while (!call.isDone()) {
            call.waitForComplete(TIME_OUT, TimeUnit.MILLISECONDS);
        }
        calls.remove(call.getRequest().getId());
    }

    @Override
    public void ready() {
        synchronized (synchronization) {
            state = STATE_RUNNING;
            synchronization.notifyAll();
        }
    }

    @Override
    public void connect() throws Exception {
        bootstrap.connect(remoteAddress).sync();
        synchronized (synchronization) {
            while (state != STATE_RUNNING && !Thread.interrupted()) {
                synchronization.wait();
            }
        }
    }


    @Override
    public void completeCall(Packet packet) {

        Call call = calls.get(packet.getId());
        if (call == null) {
            return;
        }
        call.setResponse(packet);
        call.setException(packet.getException() == null? null : new ExecuteException(packet.getException()));
        call.complete();
    }

    @Override
    public void shutdown() {
        synchronized (synchronization) {
            state = STATE_CLOSED;
        }
    }

    @Override
    public void close() {
        synchronized (synchronization) {
            if (state == STATE_RUNNING) {
                state = STATE_CONNECTING;
                Map<Long, Call> oldCalls = new HashMap<>(calls);

                connectThread.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            connect();
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
        callWriter.writeCall(call);
    }

    public void setCallWriter(CallWriter callWriter) {
        this.callWriter = callWriter;
    }
}
