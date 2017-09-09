package com.zhuyiren.rpc.spring;

import com.zhuyiren.rpc.common.ZRpcPropertiesConstant;
import com.zhuyiren.rpc.engine.Engine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhuyiren
 * @date 2017/8/14
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZRpcService {


    String value() default "";

    String client() default "";

    Class<? extends Engine> engine() default com.zhuyiren.rpc.engine.ProtostuffEngine.class;

    String[] providers() default "";

    String serviceName() default "";
}
