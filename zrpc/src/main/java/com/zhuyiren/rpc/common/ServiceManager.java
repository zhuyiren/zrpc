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

import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

/**
 * @author zhuyiren
 * @date 2017/8/21
 */
public interface ServiceManager {

    List<ProviderLoadBalanceConfig> extractProviderAddress(String serviceName) throws Exception;

    boolean watchService(String serviceName);

    List<SocketAddress> getServiceProviderAddress(String serviceName);

    List<ProviderLoadBalanceConfig> getProviderLoadBalanceConfigs(String serviceName);


}
