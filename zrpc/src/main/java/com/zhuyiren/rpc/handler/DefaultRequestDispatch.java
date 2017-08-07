package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhuyiren on 2017/5/19.
 */

public class DefaultRequestDispatch extends ChannelInboundHandlerAdapter implements RequestDispatcher {


    RequestHandlerAdapter handlerAdapter;
    Map<String,Object> serviceHandlers=new HashMap<>();

    public DefaultRequestDispatch(RequestHandlerAdapter handlerAdapter){
        this.handlerAdapter=handlerAdapter;
    }

    public DefaultRequestDispatch(){
        this.handlerAdapter=new RequestHandlerAdapterComposite();
    }

    @Override
    public void registerService(String serviceName, Object handler) {
        serviceHandlers.put(serviceName,handler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Packet request=(Packet) msg;
        Object handler=serviceHandlers.get(request.getServiceName());
        Packet response = doDispatch(request, handler);
        ctx.writeAndFlush(response);
    }

    @Override
    public Packet doDispatch(Packet request, Object handler) {
        return handlerAdapter.handle(request, handler);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }


    @Override
    public RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter) {
        handlerAdapter.addHandlerAdapter(adapter);
        return handlerAdapter;
    }
}
