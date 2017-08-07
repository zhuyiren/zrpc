package com.zhuyiren.rpc.exception;

/**
 * Created by zhuyiren on 2017/6/17.
 */
public abstract class RpcException extends RuntimeException {


    public RpcException(String msg){
        super(msg);
    }
}
