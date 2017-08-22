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
import com.zhuyiren.rpc.exception.TimeoutException;

import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public interface CallHandler {


    void call(Call call) throws TimeoutException;

    void ready();

    void connect(SocketAddress address) throws InterruptedException;

    void completeCall(Packet packet);

    void shutdown();

    void close();

    void writeCall(Call call);

    void setCallWriter(CallWriter callWriter);

    boolean setServiceState(String serviceName,boolean state);


    SocketAddress getRemoteAddress();

}
