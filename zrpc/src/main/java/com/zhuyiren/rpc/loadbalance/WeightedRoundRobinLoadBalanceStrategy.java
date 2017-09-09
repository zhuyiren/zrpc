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

import com.google.common.collect.Range;
import com.zhuyiren.rpc.common.CallHandlerManager;
import com.zhuyiren.rpc.common.ProviderLoadBalanceConfig;
import com.zhuyiren.rpc.common.ServiceManager;
import com.zhuyiren.rpc.handler.CallHandler;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhuyiren
 * @date 2017/9/4
 */
public class WeightedRoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {

    public static final String LOAD_BALANCE_TYPE="weightedRoundRobin";
    private final CallHandlerManager callHandlerManager;
    private final ServiceManager serviceManager;
    private volatile long currentIndex;
    private volatile List<ProviderWeightHolder> holders;
    private volatile int total;

    private static class ProviderWeightHolder{
        SocketAddress address;
        Range<Integer> range;

        public ProviderWeightHolder(SocketAddress address, Range<Integer> range) {
            this.address = address;
            this.range = range;
        }
    }

    public WeightedRoundRobinLoadBalanceStrategy(ServiceManager serviceManager,CallHandlerManager callHandlerManager){
        this.serviceManager=serviceManager;
        this.callHandlerManager=callHandlerManager;
        this.currentIndex=0;
    }

    @Override
    public CallHandler doSelect() {
        if(total==0){
            return null;
        }
        int slot=(int)(currentIndex++%total);
        for (ProviderWeightHolder holder : holders) {
            if(holder.range.contains(slot)){
                return callHandlerManager.getCallHandler(holder.address);
            }
        }
        return null;
    }


    @Override
    public void init(String serviceName) {
        List<ProviderLoadBalanceConfig> loadBalanceConfigs = serviceManager.getProviderLoadBalanceConfigs(serviceName);
        List<ProviderWeightHolder> holders=new CopyOnWriteArrayList<>();
        int sum=0;
        for (ProviderLoadBalanceConfig config : loadBalanceConfigs) {
            int weight = Integer.parseInt(config.getLoadBalanceProperty());
            Range<Integer> range = Range.closedOpen(sum, sum + weight);
            ProviderWeightHolder holder = new ProviderWeightHolder(config.getAddress(), range);
            holders.add(holder);
            sum+=weight;
        }
        total=sum;
        this.holders=holders;
    }


    @Override
    public String getType() {
        return LOAD_BALANCE_TYPE;
    }
}
