package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.utils.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuyiren on 2017/6/29.
 */
public class CommonRequestHandlerAdapter implements RequestHandlerAdapter {


    private List<Engine> engines;

    public  CommonRequestHandlerAdapter(){
        engines=new ArrayList<>();
    }

    @Override
    public Packet handle(Packet request, Object handler) {
        String exception;
        if (handler == null) {
            exception = "not found the service";
        } else {
            try {
                Engine engine=findEngine(request.getType());
                ArgumentHelper argumentHelper = engine.decodeArgument(request.getEntity());
                Object[] arguments = argumentHelper.arguments;
                Class[] classes = argumentHelper.argumentClasses;
                Method method = getMethod(handler.getClass(), request.getMethodName(), classes);
                Object result = method.invoke(handler, arguments);
                Packet response = new Packet(request);
                byte[] responseBytes;
                WrapReturn wrapReturn=new WrapReturn(result);
                responseBytes = engine.encodeResult(wrapReturn);
                response.setEntity(responseBytes);
                return response;
            } catch (IllegalAccessException e) {
                exception = e.getMessage();
            } catch (InvocationTargetException e) {
                exception = e.getCause().getMessage();
            } catch (Exception e) {
                exception = e.getMessage();
            }
        }
        Packet response = new Packet(request);
        response.setException(exception);
        return response;
    }

    @Override
    public boolean support(Packet request) {
        String type = request.getType();
        for (Engine engine : engines) {
            if (engine.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }


    private Method getMethod(Class<?> clz, String methodName, Class[] argumentClass) {
        Method method = null;
        for (Method item : clz.getMethods()) {
            if (item.getName().equals(methodName)) {
                if (ClassUtils.compareMethodArguments(item, argumentClass)) {
                    method = item;
                    break;
                }
            }
        }
        return method;
    }


    @Override
    public RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter) {
        throw new UnsupportedOperationException();
    }


    private Engine findEngine(String type){
        for (Engine engine : engines) {
            if(engine.getType().equals(type)){
                return engine;
            }
        }
        return null;
    }


    public void addEngine(Engine engine){
        Engine original = findEngine(engine.getType());
        if(original!=null){
            return;
        }
        engines.add(engine);
    }




}
