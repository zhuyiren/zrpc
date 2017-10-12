package com.zhuyiren.rpc.loadbalance;

import com.zhuyiren.rpc.handler.CallHandler;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public interface LoadBalanceStrategy<T> {


    void init(String serviceName);

    CallHandler doSelect();

    String getType();

    void update(T object);

}
