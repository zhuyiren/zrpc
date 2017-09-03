package com.zhuyiren.rpc.handler;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public interface LoadBalanceStrategy {


    CallHandler doSelect(String serviceName);

}
