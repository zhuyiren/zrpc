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

import com.zhuyiren.rpc.common.CallHandlerManager;
import com.zhuyiren.rpc.common.ServiceManager;

import java.net.SocketAddress;
import java.util.List;

/**
 * @author zhuyiren
 * @date 2017/9/3
 */
public class RoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {


    private final ServiceManager serviceManager;
    private final CallHandlerManager callHandlerManager;
    private volatile long currentIndex;

    public RoundRobinLoadBalanceStrategy(ServiceManager serviceManager,CallHandlerManager callHandlerManager){
        this.serviceManager=serviceManager;
        this.callHandlerManager=callHandlerManager;
        currentIndex=0;
    }



    @Override
    public CallHandler doSelect(String serviceName) {
        List<SocketAddress> providerAddress = serviceManager.getServiceProviderAddress(serviceName);
        int length = providerAddress.size();
        if(length==0){
            return null;
        }
        return callHandlerManager.getCallHandler(providerAddress.get((int) (currentIndex++ % length)));
    }
}
