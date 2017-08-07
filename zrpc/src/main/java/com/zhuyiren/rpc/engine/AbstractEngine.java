package com.zhuyiren.rpc.engine;

import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.handler.Call;
import com.zhuyiren.rpc.handler.CallHandler;
import com.zhuyiren.rpc.handler.Invoker;

import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public abstract class AbstractEngine implements Engine {

    public AbstractEngine(){
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Engine)) {
            return false;
        }
        Engine other = (Engine) obj;

        return getType().equals(other.getType());
    }
}
