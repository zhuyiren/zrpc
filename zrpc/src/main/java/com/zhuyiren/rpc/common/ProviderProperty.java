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

import java.net.SocketAddress;

/**
 * @author zhuyiren
 * @date 2017/9/4
 */
public class ProviderProperty {

    private final SocketAddress address;
    private final String loadBalanceType;
    private final String loadBalanceProperty;

    public ProviderProperty(SocketAddress address, String loadBalanceType, String loadBalanceProperty) {
        this.address = address;
        this.loadBalanceType=loadBalanceType;
        this.loadBalanceProperty = loadBalanceProperty;
    }


    public SocketAddress getAddress() {
        return address;
    }

    public String getLoadBalanceProperty() {
        return loadBalanceProperty;
    }

    public String getLoadBalanceType(){
        return loadBalanceType;
    }


    @Override
    public String toString() {
        return "ProviderProperty{" +
                "address=" + address +
                ", loadBalanceType='" + loadBalanceType + '\'' +
                ", loadBalanceProperty='" + loadBalanceProperty + '\'' +
                '}';
    }
}
