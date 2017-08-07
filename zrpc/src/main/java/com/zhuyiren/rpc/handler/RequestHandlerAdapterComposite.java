package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.Packet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuyiren on 2017/5/19.
 */
public class RequestHandlerAdapterComposite implements RequestHandlerAdapter {


    List<RequestHandlerAdapter> handlerAdapters=new ArrayList<>();

    public RequestHandlerAdapterComposite(){

    }

    @Override
    public Packet handle(Packet request, Object handler) {
        for (RequestHandlerAdapter item : handlerAdapters) {
            if(item.support(request)){
                return item.handle(request,handler);
            }
        }
        String exception="no such engine type";
        Packet packet = new Packet(request);
        packet.setException(exception);
        return packet;
    }

    @Override
    public boolean support(Packet request) {
        for (RequestHandlerAdapter handlerAdapter : handlerAdapters) {
            if(handlerAdapter.support(request)){
                return true;
            }
        }
        return false;
    }

    @Override
    public RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter) {
        this.handlerAdapters.add(adapter);
        return this;
    }
}
