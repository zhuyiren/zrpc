package com.zhuyiren.rpc.common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhuyiren on 2017/8/7.
 */
public class ClientIdleHandler extends ChannelInboundHandlerAdapter {


    private ScheduledFuture<?> future;
    private ScheduledExecutorService connectThread;

    private static final Packet PING_PACKET;

    private static final String PING_TYPE="ping";
    static {
        PING_PACKET=new Packet();
        PING_PACKET.setType(PING_TYPE);
    }

    public ClientIdleHandler(ScheduledExecutorService connectThread){
        this.connectThread=connectThread;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        future=ctx.executor().scheduleWithFixedDelay(() -> ctx.writeAndFlush(PING_PACKET),5,5, TimeUnit.SECONDS);
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
