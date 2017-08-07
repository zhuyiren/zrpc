package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.Packet;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public interface RequestHandlerAdapter {



    Packet handle(Packet request, Object handler);


    boolean support(Packet request);


    RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter);
}
