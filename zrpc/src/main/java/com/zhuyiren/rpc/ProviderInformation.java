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

package com.zhuyiren.rpc;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhuyiren
 * @date 2017/8/8
 */
public class ProviderInformation {

    private static final String SEPARATOR = System.getProperty("line.separator");

    private SocketAddress address;
    private boolean isStart;
    private Map<String, Object> services;

    public ProviderInformation(SocketAddress address) {
        this.address = address;
        services = new HashMap<>();
        isStart = false;
    }


    public SocketAddress getAddress() {
        return address;
    }


    public boolean addService(String serviceName, Object handler) {
        if (services.containsKey(serviceName)) {
            return false;
        } else {
            services.put(serviceName, handler);
            return true;
        }
    }

    public Map<String, Object> getServices() {
        return services;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("服务信息:")
                .append(SEPARATOR)
                .append("address:").append(address)
                .append(SEPARATOR);
        for (Map.Entry<String, Object> entry : services.entrySet()) {
            sb.append("service name:")
                    .append(entry.getKey())
                    .append("---service handler:")
                    .append(entry.getValue())
                    .append(SEPARATOR);
        }
        return sb.toString();
    }


    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }
}
