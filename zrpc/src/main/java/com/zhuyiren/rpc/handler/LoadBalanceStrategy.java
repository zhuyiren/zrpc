package com.zhuyiren.rpc.handler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public interface LoadBalanceStrategy {


    void init(String serviceName);

    CallHandler doSelect();

    String getType();

}
