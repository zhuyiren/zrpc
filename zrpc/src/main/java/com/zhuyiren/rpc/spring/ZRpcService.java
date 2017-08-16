package com.zhuyiren.rpc.spring;

import com.zhuyiren.rpc.engine.Engine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhuyiren
 * @date 2017/8/14
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZRpcService {


    String value() default "";

    Class<?> ifcCls() default Object.class;

    String client() default "client";

    Class<? extends Engine> engine() default com.zhuyiren.rpc.engine.ProtostuffEngine.class;

    String host();

    int port() default 3324;

    boolean cache() default false;

    String serviceName() default "";
}
