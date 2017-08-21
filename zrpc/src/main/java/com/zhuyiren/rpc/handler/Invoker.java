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

import com.zhuyiren.rpc.engine.Engine;

import java.lang.reflect.InvocationHandler;

/**
 * 代理调用类，一个服务，一个该实例。当代理调用发生的时候，会对数据进行参数化处理。
 * Created by zhuyiren on 2017/6/3.
 */
public interface Invoker extends InvocationHandler {


    void setCallHandler(CallHandler callHandler);

    void setServiceName(String serviceName);

    void setEngine(Engine engine);



}
