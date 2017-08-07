package com.zhuyiren.rpc;

import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.exception.TimeoutExcepiton;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public interface Client {


    <T> T exportService(Class<? extends Engine> engineType, Class<T> service, SocketAddress address, boolean useCache) throws Exception;

    <T> T exportService(Class<? extends Engine> engineType, Class<T> service,String serviceName, SocketAddress address, boolean useCache) throws Exception;

    void registerEngine(Engine engine);

    void shutdown();
}
