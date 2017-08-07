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

/**
 * Created by zhuyiren on 2017/8/4.
 */
public class DefaultInvoker implements Invoker {


    private CallHandler callHandler;
    private String serviceName;
    private Engine engine;

    public DefaultInvoker(String serviceName){
        this.serviceName=serviceName;
    }

    public void setCallHandler(CallHandler callHandler) {
        this.callHandler = callHandler;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        byte[] requestEntity = engine.encodeArgument(args);
        Packet request=new Packet(serviceName,engine.getType(),method.getName(),requestEntity);
        Call call=new Call(request);
        callHandler.call(call);
        if(call.getException()!=null){
            throw call.getException();
        }
        WrapReturn wrapReturn = engine.decodeResult(call.getResponse().getEntity());
        return wrapReturn.getResult();
    }


    @Override
    public void setEngine(Engine engine) {
        this.engine=engine;
    }
}
