package com.zhuyiren.rpc.handler;


import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.exception.ExecuteException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.ExecutionException;

/**
 * Created by zhuyiren on 2017/4/11.
 */
public class RequestHandler extends ChannelInboundHandlerAdapter implements CallWriter {


    private volatile ChannelHandlerContext context;
    private CallHandler callHandler;

    public RequestHandler() {

    }

    public RequestHandler(CallHandler callHandler) {
        this.callHandler = callHandler;
    }


    public void writeCall(Call call) {
        context.writeAndFlush(call.getRequest()).addListener(future -> {
            if(future.cause()!=null){
                call.setException(new ExecuteException(future.cause().getMessage()));
                synchronized (call){
                    call.complete();
                }
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        context = ctx;
        callHandler.ready();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Packet response = (Packet) msg;
        callHandler.completeCall(response);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        callHandler.close();
        ctx.fireChannelInactive();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);

    }


}
