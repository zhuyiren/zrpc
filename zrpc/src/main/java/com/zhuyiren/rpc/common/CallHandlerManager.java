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

import com.zhuyiren.rpc.handler.CallHandler;

import java.net.SocketAddress;

/**
 * @author zhuyiren
 * @date 2017/8/21
 */
public interface CallHandlerManager {

    CallHandler createCaller(SocketAddress address) throws Exception;

    CallHandler removeCaller(CallHandler callHandler);

    CallHandler getCallHandler(SocketAddress address);
}
