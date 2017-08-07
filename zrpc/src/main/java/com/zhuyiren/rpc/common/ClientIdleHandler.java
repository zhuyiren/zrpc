package com.zhuyiren.rpc.common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhuyiren on 2017/8/7.
 */
public class ClientIdleHandler extends ChannelInboundHandlerAdapter {


    private ScheduledFuture<?> future;

    private static final Packet PING_PACKET;

    private static final long PEROID=5000;


    static {
        PING_PACKET=new Packet();
        PING_PACKET.setType(CommonConstant.IDLE_PING);
    }

    public ClientIdleHandler(){
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        future=ctx.executor().scheduleWithFixedDelay(() -> {
            ctx.writeAndFlush(PING_PACKET);
        },PEROID,PEROID, TimeUnit.MILLISECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(future!=null){
            future.cancel(false);
        }
        super.channelInactive(ctx);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            return;
        }

        if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
            ctx.close();
        }
    }
}
