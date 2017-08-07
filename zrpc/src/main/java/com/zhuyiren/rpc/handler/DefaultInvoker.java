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
