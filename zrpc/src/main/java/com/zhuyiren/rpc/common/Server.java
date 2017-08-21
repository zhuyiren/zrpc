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

import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public interface Server {

    boolean register(String serviceName, Object handler,String host,int port);

    boolean register(String serviceName,Object handler,int port);

    boolean register(String serviceName,Object handler);

    boolean start(SocketAddress address) throws IllegalArgumentException;

    boolean shutdown();

    Map<String,Object> getServices(SocketAddress address);

    EventExecutorGroup getBusinessExecutors();

}