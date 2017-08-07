package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.engine.Engine;

import java.lang.reflect.InvocationHandler;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public interface Invoker extends InvocationHandler {


    void setCallHandler(CallHandler callHandler);

    void setServiceName(String serviceName);


    void setEngine(Engine engine);

}
