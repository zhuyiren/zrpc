package com.zhuyiren.rpc.handler;


import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.exception.TimeoutExcepiton;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public interface CallHandler {


    void call(Call call) throws TimeoutExcepiton;

    void ready();

    void connect() throws Exception;

    void completeCall(Packet packet);

    void shutdown();

    void close();

    void writeCall(Call call);

    void setCallWriter(CallWriter callWriter);


}
