package com.zhuyiren.rpc.utils;

import java.lang.reflect.Method;

/**
 * Created by zhuyiren on 2017/5/20.
 */
public class ClassUtils {
    
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
     * @param clz1
     * @param clz2 为普通类型或者装箱类型
     * @return
     */
    private static boolean compareClass(Class clz1, Class clz2) {
        if (clz1.equals(clz2)) {
            return true;
        }


        //判断是否装箱类型
        if (clz1.equals(char.class) && clz2.equals(Character.class)) {
            return true;
        }
        if (clz1.equals(boolean.class) && clz2.equals(Boolean.class)) {
            return true;
        }
        if (clz1.equals(byte.class) && clz2.equals(Byte.class)) {
            return true;
        }
        if (clz1.equals(short.class) && clz2.equals(Short.class)) {
            return true;
        }
        if (clz1.equals(float.class) && clz2.equals(Float.class)) {
            return true;
        }
        if (clz1.equals(int.class) && clz2.equals(Integer.class)) {
            return true;
        }
        if (clz1.equals(double.class) && clz2.equals(Double.class)) {
            return true;
        }
        if (clz1.equals(long.class) && clz2.equals(Long.class)) {
            return true;
        }

        return false;

    }


    public static Class<?> getWrapClass(Class primitiveClass){
        if(primitiveClass.equals(void.class)){
            return Void.class;
        }else if(primitiveClass.equals(byte.class)){
            return Byte.class;
        }else if(primitiveClass.equals(char.class)){
            return Character.class;
        }else if(primitiveClass.equals(short.class)){
            return Short.class;
        }else if(primitiveClass.equals(int.class)){
            return Integer.class;
        }else if(primitiveClass.equals(long.class)){
            return Long.class;
        }else if(primitiveClass.equals(float.class)){
            return Float.class;
        }else if(primitiveClass.equals(double.class)){
            return Double.class;
        }
        throw new IllegalArgumentException(primitiveClass+" is not a primitive type");
    }
}
