package com.zhuyiren.rpc.handler;

import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by zhuyiren on 2017/5/18.
 */

@ChannelHandler.Sharable
public class StatisticsHandler extends ChannelOutboundHandlerAdapter implements ChannelInboundHandler {

    private static final Logger LOGGER= LoggerFactory.getLogger(StatisticsHandler.class);

    private AtomicLong count = new AtomicLong();
    private AtomicLong channels = new AtomicLong();
    private long preCount = 0;
    private long preTime = System.currentTimeMillis();
    private volatile AtomicBoolean isStart = new AtomicBoolean(false);

    public StatisticsHandler() {
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        count.incrementAndGet();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();

        synchronized (isStart) {
            if (isStart.get() == false) {
                ctx.executor().scheduleWithFixedDelay(() -> {
                    long currentCount = count.get();
                    long currentTime = System.currentTimeMillis();
                    LOGGER.info("Statistics:" + (currentCount - preCount) * 1000 / (currentTime - preTime) + "   Clients:" + channels.get() + "   sum:" + currentCount);
                    preCount = currentCount;
                    preTime = currentTime;
                }, 1, 1, TimeUnit.SECONDS);
                isStart.set(true);
            }
        }

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        channels.incrementAndGet();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        channels.decrementAndGet();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }
}
