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

import com.google.common.collect.ImmutableList;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public class ServiceInformation {

    private final String serviceName;
    private final boolean isManageByZookeeper;
    private final LoadBalanceType loadBalanceType;
    private final ImmutableList<SocketAddress> addresses;

    public ServiceInformation(String serviceName, LoadBalanceType loadBalanceType,boolean isManageByZookeeper, List<SocketAddress> addresses) {
        this.serviceName = serviceName;
        this.loadBalanceType=loadBalanceType;
        this.isManageByZookeeper = isManageByZookeeper;
        if(addresses==null){
            addresses=new ArrayList<>();
        }
        this.addresses = ImmutableList.copyOf(addresses);

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceInformation)) return false;
        ServiceInformation that = (ServiceInformation) o;
        return Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName);
    }


    public String getServiceName() {
        return serviceName;
    }

    public boolean isManageByZookeeper() {
        return isManageByZookeeper;
    }

    public ImmutableList<SocketAddress> getAddresses() {
        return addresses;
    }

    public LoadBalanceType getLoadBalanceType() {
        return loadBalanceType;
    }
}
