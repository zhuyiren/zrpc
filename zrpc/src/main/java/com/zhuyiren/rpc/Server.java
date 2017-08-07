package com.zhuyiren.rpc;

import com.zhuyiren.rpc.engine.Engine;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Map;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public interface Server {

    void register(String serviceName, Object handler);
    void start(int port);
    void shutdown();
    Map<String,Object> getServices();
    EventExecutorGroup getBussinessExecutors();
    void registerEngine(Engine engine);
}
