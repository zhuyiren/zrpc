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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhuyiren
 * @date 2017/10/12
 */
public class AverageCallTimeStrategy implements LoadBalanceStrategy<CallDuration> {

    public static final String LOAD_BALANCE_TYPE = "average";

    private final ServiceManager serviceMgr;
    private final CallHandlerManager callHandlerMgr;
    private final ConcurrentHashMap<CallHandler, CallTimeStatistics> callTimeStatisticsMap = new ConcurrentHashMap<>();


    public static class CallTimeStatistics implements Comparable<CallTimeStatistics>{
        private final CallHandler callHandler;
        private volatile long times;
        private volatile long average;

        public CallTimeStatistics(CallHandler callHandler) {
            this.callHandler = callHandler;
            this.times = 0;
            this.average = 0;
        }

        public void increment(long duration) {
            average = (times * this.average + duration) / (++times);
        }

        @Override
        public int compareTo(CallTimeStatistics o) {
            return (int) (this.average - o.average);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CallTimeStatistics)) return false;
            CallTimeStatistics that = (CallTimeStatistics) o;
            return callHandler.equals(that.callHandler);
        }

        @Override
        public int hashCode() {
            return callHandler.hashCode();
        }
    }

    public AverageCallTimeStrategy(ServiceManager serviceMgr, CallHandlerManager callHandlerManager) {
        this.serviceMgr = serviceMgr;
        this.callHandlerMgr = callHandlerManager;
    }


    @Override
    public void init(String serviceName) {
        List<SocketAddress> serviceAddresses = serviceMgr.getServiceProviderAddress(serviceName);
        serviceAddresses.stream().map((socketAddress) ->
                callHandlerMgr.getCallHandler(socketAddress)
        ).forEach(item -> callTimeStatisticsMap.put(item, new CallTimeStatistics(item)));
    }

    @Override
    public CallHandler doSelect() {
        Collection<CallTimeStatistics> statistics = callTimeStatisticsMap.values();
        CallTimeStatistics min = Collections.min(statistics);
        return min.callHandler;
    }

    @Override
    public String getType() {
        return LOAD_BALANCE_TYPE;
    }

    @Override
    public void update(CallDuration duration) {
        callTimeStatisticsMap.get(duration.callHandler).increment(duration.duration);
    }
}
