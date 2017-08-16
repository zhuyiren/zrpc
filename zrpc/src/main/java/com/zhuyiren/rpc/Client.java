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

import java.net.SocketAddress;

/**
 * RPC客户端，用于
 * Created by zhuyiren on 2017/6/3.
 */
public interface Client {


    <T> T exportService(Class<? extends Engine> engineType, Class<T> service, SocketAddress address, boolean useCache) throws Exception;

    <T> T exportService(Class<? extends Engine> engineType, Class<T> service,String serviceName, SocketAddress address, boolean useCache) throws Exception;


    Engine addEngineByClass(Class<? extends Engine> engineClass) throws Exception;

    Engine addEngine(Engine engine);

    void shutdown();
}
