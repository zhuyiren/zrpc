package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.Packet;

/**
 * Created by zhuyiren on 2017/5/19.
 */
public interface RequestDispatcher {



    Packet doDispatch(Packet request, Object handler);

    void registerService(String serviceName, Object handler);

    RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter);
}
