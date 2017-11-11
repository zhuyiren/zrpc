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

package com.zhuyiren.rpc.loadbalance;

import com.zhuyiren.rpc.common.CallHandlerManager;
import com.zhuyiren.rpc.common.ServiceManager;
import com.zhuyiren.rpc.handler.CallHandler;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhuyiren
 * @date 2017/9/3
 */
public class RoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {

    public static final String LOAD_BALANCE_TYPE="roundRobin";

    private final ServiceManager serviceManager;
    private final CallHandlerManager callHandlerMgr;
    private volatile List<SocketAddress> addresses;
    private volatile long currentIndex;

    public RoundRobinLoadBalanceStrategy(ServiceManager serviceManager,CallHandlerManager callHandlerMgr){
        this.serviceManager=serviceManager;
        this.callHandlerMgr = callHandlerMgr;
        currentIndex=0;
    }

    @Override
    public void init(String serviceName) {
        List<SocketAddress> temp = serviceManager.getServiceProviderAddress(serviceName);
        addresses=new CopyOnWriteArrayList<>(temp);
    }

    @Override
    public String getType() {
        return LOAD_BALANCE_TYPE;
    }

    @Override
    public CallHandler doSelect() {

        if(addresses.isEmpty()){
            return null;
        }
        int length = addresses.size();
        return callHandlerMgr.getCallHandler(addresses.get((int) (currentIndex++ % length)));
    }

    @Override
    public void update(Object object) {
        return;
    }
}
