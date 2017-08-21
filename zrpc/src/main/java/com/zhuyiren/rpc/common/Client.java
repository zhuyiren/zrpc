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

import java.net.SocketAddress;

/**
 * RPC客户端，导出服务和序列化引擎在这里管理。
 * Created by zhuyiren on 2017/6/3.
 */
public interface Client {


    /**
     * 导出服务类
     * @param engineType 序列化引擎
     * @param service 服务接口类型，必须是{@code interface}类型，否则抛出异常
     * @param address 服务地址
     * @param useCache 当服务地址相同的时候，是否采用缓存机制。如果是，则采用同一个连接，否则，重新开启新的连接
     * @return 如果成功，返回服务,否则抛出异常
     * @throws Exception
     */
    <T> T exportService(Class<? extends Engine> engineType, Class<T> service, SocketAddress address, boolean useCache) throws Exception;


    /**
     *
     * @param engineType 序列化引擎
     * @param service 服务接口类型，必须是{@code interface}类型，否则抛出异常
     * @param serviceName 服务名称
     * @param address 服务地址
     * @param useCache 当服务地址相同的时候，是否采用缓存机制。如果是，则采用同一个连接，否则，重新开启新的连接
     * @return 如果成功，返回服务,否则抛出异常
     * @throws Exception
     */
    <T> T exportService(Class<? extends Engine> engineType, Class<T> service,String serviceName, SocketAddress address, boolean useCache) throws Exception;


    Engine addEngineByClass(Class<? extends Engine> engineClass) throws Exception;

    Engine addEngine(Engine engine);

    void shutdown();


}
