package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.exception.RpcException;
import com.zhuyiren.rpc.exception.TimeoutExcepiton;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by zhuyiren on 2017/4/17.
 */
public class Call {

    private Packet request;
    private Packet response;
    private volatile boolean done;
    private RpcException exception;
    private Thread thread;

    public Call(Packet request) {
        this(request, null);
    }

    public Call(Packet request, Packet response) {
        this.request = request;
        this.response = response;
        done = false;
    }

    public Packet getRequest() {
        return request;
    }

    public void setRequest(Packet request) {
        this.request = request;
    }

    public Packet getResponse() {
        return response;
    }

    public void setResponse(Packet response) {
        this.response = response;
    }

    public boolean isDone() {
        return done;
    }

    public void complete() {
        done = true;
        LockSupport.unpark(thread);
    }

    public RpcException getException() {
        return exception;
    }

    public void setException(RpcException exception) {
        this.exception = exception;
    }

    public void waitForComplete(int timeout, TimeUnit unit) {

        thread = Thread.currentThread();
        LockSupport.parkNanos(unit.toNanos(timeout));
        if (response == null) {
            exception = new TimeoutExcepiton("Time out");
            done = true;
        }
    }


    @Override
    public String toString() {
        return "Call{" +
                "request=" + request +
                ", response=" + response +
                ", done=" + done +
                '}';
    }
}
