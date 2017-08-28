package com.zhuyiren.rpc.spring;

import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhuyiren
 * @date 2017/8/12
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface ZRpcProvider {

    String value() default "";

    String serviceName() default "";

    String host() default "";

    int port() default 3324;

    String server() default "server";

}
