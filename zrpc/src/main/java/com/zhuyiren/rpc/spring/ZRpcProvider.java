package com.zhuyiren.rpc.spring;

import com.zhuyiren.rpc.common.ZRpcPropertiesConstant;
import com.zhuyiren.rpc.loadbalance.RandomLoadBalanceStrategy;
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

    String providerInfo() default "";

    String server() default "";

}
