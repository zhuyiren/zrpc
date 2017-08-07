package com.zhuyiren.rpc.engine;

import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.sun.org.apache.regexp.internal.RE;
import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHelper;
import com.zhuyiren.rpc.handler.Invoker;

import java.lang.reflect.Method;
import java.net.SocketAddress;

/**
 * Created by zhuyiren on 2017/6/29.
 */
public class ProtobufEngine extends AbstractEngine {


    public static final String ENGINE_PROTOBUF="pb";


    @Override
    public String getType() {
        return ENGINE_PROTOBUF;
    }




    @Override
    public byte[] encodeArgument(Object[] arguments) throws Exception {
        return ((MessageLite) arguments[0]).toByteArray();
    }

    @Override
    public ArgumentHelper decodeArgument(byte[] inBytes) throws Exception {
        return null;
    }

    @Override
    public byte[] encodeResult(WrapReturn result) throws Exception {
        return new byte[0];
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes) throws Exception {
        return null;
    }
}
