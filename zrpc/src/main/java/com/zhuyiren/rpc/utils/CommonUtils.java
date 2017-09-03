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

package com.zhuyiren.rpc.utils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import static com.zhuyiren.rpc.common.ZRpcPropertiesConstant.ANY_HOST;

/**
 * @author zhuyiren
 * @date 2017/9/3
 */
public final class CommonUtils {


    private CommonUtils(){

    }

    public static void checkNoAnyHost(List<SocketAddress> addresses) {
        addresses.forEach(address -> {
            if (address != null && address instanceof InetSocketAddress) {
                if (ANY_HOST.equals(((InetSocketAddress) address).getAddress().getHostAddress())) {
                    throw new IllegalArgumentException("The address must not be 0.0.0.0");
                }
            }
        });
    }
}
