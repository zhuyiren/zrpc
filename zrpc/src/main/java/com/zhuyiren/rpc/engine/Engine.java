package com.zhuyiren.rpc.engine;


import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHelper;
import com.zhuyiren.rpc.handler.Invoker;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketAddress;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public interface Engine {


    String getType();



    byte[] encodeArgument(Object[] arguments) throws Exception;

    ArgumentHelper decodeArgument(byte[] inBytes) throws Exception;


    byte[] encodeResult(WrapReturn result) throws Exception;

    WrapReturn decodeResult(byte[] inBytes) throws Exception;



}
