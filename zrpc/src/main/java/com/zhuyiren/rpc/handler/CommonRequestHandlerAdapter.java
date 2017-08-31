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
import com.zhuyiren.rpc.exception.RpcNoSuchEngineException;
import com.zhuyiren.rpc.utils.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by zhuyiren on 2017/6/29.
 */
public class CommonRequestHandlerAdapter implements RequestHandlerAdapter {


    private final Map<String, Engine> engineMap;
    private final Map<MethodHolder, Method> methodMap;


    public CommonRequestHandlerAdapter(List<Engine> engines) {
        engineMap = new HashMap<>();
        methodMap = new HashMap<>();

        if (engines == null) {
            return;
        }
        for (Engine engine : engines) {
            engineMap.put(engine.getType(), engine);
        }
    }

    @Override
    public Packet handle(Packet request, Object handler) {
        String exception;
        if (handler == null) {
            exception = "not found the service";
        } else {
            try {
                Engine engine = findEngine(request.getType());
                ArgumentHolder argumentHolder = engine.decodeArgument(request.getEntity());
                Object[] arguments = argumentHolder.arguments;
                Class[] classes = argumentHolder.argumentClasses;
                Method method = getMethod(handler.getClass(), request.getMethodName(), classes);
                Object result = method.invoke(handler, arguments);
                Packet response = new Packet(request);
                byte[] responseBytes;
                WrapReturn wrapReturn = new WrapReturn(result);
                responseBytes = engine.encodeResult(wrapReturn);
                response.setEntity(responseBytes);
                return response;
            } catch (IllegalAccessException e) {
                exception = e.getMessage();
            } catch (InvocationTargetException e) {
                exception = e.getCause().getMessage();
            } catch (RpcNoSuchEngineException e) {
                exception = e.getMessage();
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
        return findEngine(request.getType()) != null;
    }


    private Method getMethod(Class<?> clz, String methodName, Class[] argumentClass) {
        MethodHolder methodHolder = new MethodHolder(clz, methodName, argumentClass);
        return methodMap.computeIfAbsent(methodHolder, key -> {
            for (Method item : clz.getMethods()) {
                if (item.getName().equals(methodName)) {
                    if (ClassUtils.compareMethodArguments(item, argumentClass)) {
                        return item;
                    }
                }
            }
            return null;
        });
    }


    @Override
    public RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter) {
        throw new UnsupportedOperationException();
    }


    private Engine findEngine(String type) {
        return engineMap.get(type);
    }


    private final static class MethodHolder {
        private final Class<?> targetClass;
        private final String methodName;
        private final Class<?>[] argumentClasses;
        private final int hash;

        public MethodHolder(Class<?> targetClass, String methodName, Class<?>[] argumentClasses) {
            this.targetClass = targetClass;
            this.methodName = methodName;
            this.argumentClasses = argumentClasses;
            hash = evaluateHash(targetClass, methodName, argumentClasses);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodHolder)) return false;
            MethodHolder that = (MethodHolder) o;
            return Objects.equals(targetClass, that.targetClass) &&
                    Objects.equals(methodName, that.methodName) &&
                    Arrays.equals(argumentClasses, that.argumentClasses);
        }

        @Override
        public int hashCode() {
            return hash;
        }


        private int evaluateHash(Class<?> targetClass, String methodName, Class<?>[] argumentClasses) {
            int hash = Objects.hash(targetClass, methodName);
            return hash * 31 + Arrays.hashCode(argumentClasses);
        }
    }


}
