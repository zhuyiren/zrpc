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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhuyiren on 2017/5/20.
 */
public class ClassUtils {

    private static final Map<Class<?>,Class<?>> primitiveMapped;

    static {
        primitiveMapped=new HashMap<>();
        primitiveMapped.put(char.class,Character.class);
        primitiveMapped.put(byte.class,Byte.class);
        primitiveMapped.put(boolean.class,Boolean.class);
        primitiveMapped.put(short.class,Short.class);
        primitiveMapped.put(int.class,Integer.class);
        primitiveMapped.put(float.class,Float.class);
        primitiveMapped.put(long.class,Long.class);
        primitiveMapped.put(double.class,Double.class);
    }


    public static boolean compareMethodArguments(Method method, Class[] classes) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != classes.length) {
            return false;
        }
        for (int index = 0; index < parameterTypes.length; index++) {
            if(!compareClass(parameterTypes[index],classes[index])){
                return false;
            }
        }

        return true;
    }

    /**
     * @param clz1 为普通类型或者primitive类型
     * @param clz2 为普通类型或者装箱类型
     * @return
     */
    private static boolean compareClass(Class<?> clz1, Class<?> clz2) {
        if (clz1.isAssignableFrom(clz2)) {
            return true;
        }

        //判断是否装箱类型
        Class<?> boxedType = primitiveMapped.get(clz1);
        return boxedType!=null && boxedType.equals(clz2);

    }
}
