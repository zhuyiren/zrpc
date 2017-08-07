package com.zhuyiren.rpc.common;

/**
 * Created by zhuyiren on 2017/8/1.
 */
public class WrapReturn {


    private Object result;

    public WrapReturn(){

    }

    public WrapReturn(Object result){
        this.result=result;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
