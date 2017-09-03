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

import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.engine.Engine;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhuyiren on 2017/8/4.
 */
public class DefaultInvoker implements Invoker {


    private final LoadBalanceStrategy strategy;
    private String serviceName;
    private Engine engine;
    private Map<Method,Class[]> methodMap;

    public DefaultInvoker(String serviceName,Engine engine,LoadBalanceStrategy strategy) {
        this.serviceName = serviceName;
        this.strategy=strategy;
        this.engine=engine;
        methodMap=new ConcurrentHashMap<>();
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        final Object[] realArgument;
        if (args == null) {
            realArgument = new Object[]{};
        }else {
            realArgument=args;
        }
        Class<?>[] classes = methodMap.computeIfAbsent(method,key->{
            Class[] cacheClass = new Class[realArgument.length];
            for (int index = 0; index < realArgument.length; index++) {
                cacheClass[index] = realArgument[index].getClass();
            }
            return cacheClass;
        });
        ArgumentHolder argumentHolder = new ArgumentHolder(args, classes);
        byte[] requestEntity = engine.encodeArgument(argumentHolder);
        Packet request = new Packet(serviceName, engine.getType(), method.getName(), requestEntity);
        Call call = new Call(request);
        CallHandler callHandler = strategy.doSelect(serviceName);
        if(callHandler==null){
            throw new IllegalStateException("Can't find valid provider to do");
        }
        callHandler.call(call);
        if (call.getException() != null) {
            throw call.getException();
        }
        WrapReturn wrapReturn = engine.decodeResult(call.getResponse().getEntity(), method.getGenericReturnType());
        return wrapReturn.getResult();
    }
}
