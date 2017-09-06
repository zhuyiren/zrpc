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
import com.zhuyiren.rpc.loadbalance.LoadBalanceStrategy;

import java.util.List;

/**
 * RPC客户端，导出服务和序列化引擎在这里管理。
 * Created by zhuyiren on 2017/6/3.
 */
public interface Client {


    /**
     * 导出服务类
     * @param engineType 序列化引擎
     * @param service 服务接口类型，必须是{@code interface}类型，否则抛出异常
     * @param providers 服务地址
     * @return 如果成功，返回服务,否则抛出异常
     * @throws Exception
     */
    <T> T exportService(Class<? extends Engine> engineType, Class<T> service, List<ProviderLoadBalanceConfig> providers,String loadBalanceString) throws Exception;


    /**
     *
     * @param engineType 序列化引擎
     * @param service 服务接口类型，必须是{@code interface}类型，否则抛出异常
     * @param serviceName 服务名称
     * @param providers 服务地址
     * @return 如果成功，返回服务,否则抛出异常
     * @throws Exception
     */
    <T> T exportService(Class<? extends Engine> engineType, Class<T> service,String serviceName, List<ProviderLoadBalanceConfig> providers,String loadBalanceString) throws Exception;


    Engine addEngineByClass(Class<? extends Engine> engineClass) throws Exception;

    Engine addEngine(Engine engine);

    void shutdown();

    LoadBalanceStrategy getLoadBalanceStrategy(String type);

    void registerLoadBalance(Class<? extends LoadBalanceStrategy> strategy);


}
