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
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public class RandomLoadBalanceStrategy implements LoadBalanceStrategy {

    public static final String LOAD_BALANCE_TYPE = "random";

    private final ServiceManager serviceManager;
    private final CallHandlerManager callHandlerManager;
    private volatile List<SocketAddress> addresses;


    public RandomLoadBalanceStrategy(ServiceManager serviceManager, CallHandlerManager callHandlerManager) {
        this.serviceManager = serviceManager;
        this.callHandlerManager = callHandlerManager;
    }


    public String getType() {
        return LOAD_BALANCE_TYPE;
    }

    @Override
    public CallHandler doSelect() {

        int length = addresses.size();
        if (length == 0) {
            return null;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        SocketAddress targetAddress = addresses.get(random.nextInt(length));
        return callHandlerManager.getCallHandler(targetAddress);
    }


    @Override
    public void init(String serviceName) {
        List<SocketAddress> temp = serviceManager.getServiceProviderAddress(serviceName);
        addresses = new CopyOnWriteArrayList<>(temp);
    }

    @Override
    public void update(Object object) {
        return;
    }
}
