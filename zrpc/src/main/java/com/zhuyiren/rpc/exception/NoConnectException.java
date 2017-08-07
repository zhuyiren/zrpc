package com.zhuyiren.rpc.exception;

/**
 * Created by zhuyiren on 2017/6/24.
 */
public class NoConnectException extends RpcException {
    public NoConnectException(String msg) {
        super(msg);
    }
}
