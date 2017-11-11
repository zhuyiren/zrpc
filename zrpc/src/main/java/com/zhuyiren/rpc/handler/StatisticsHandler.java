/*
 * Copyright 2017 The ZRPC Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhuyiren.rpc.handler;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by zhuyiren on 2017/5/18.
 */

@ChannelHandler.Sharable
public class StatisticsHandler extends ChannelOutboundHandlerAdapter implements ChannelInboundHandler {

    private static final Logger LOGGER= LoggerFactory.getLogger(StatisticsHandler.class);

    private final LongAdder count = new LongAdder();
    private final LongAdder channels = new LongAdder();
    private final LongAdder readCount=new LongAdder();
    private long preCount = 0;
    private long preTime = System.currentTimeMillis();
    private long preReadCount=0;
    private volatile AtomicBoolean isStart = new AtomicBoolean(false);

    public StatisticsHandler() {
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        count.increment();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();

        if (isStart.compareAndSet(false,true)) {
            ctx.executor().scheduleWithFixedDelay(() -> {
                long currentCount = count.sum();
                long currentTime = System.currentTimeMillis();
                long currentReadCount=readCount.sum();
                long durationTime = currentTime - preTime;
                StringBuilder sb=new StringBuilder();
                sb.append("Statistics:").append((currentCount - preCount) * 1000 / durationTime)
                        .append("-----").append((currentReadCount - preReadCount) * 1000 / durationTime)
                        .append("   Clients:").append(channels.sum())
                        .append("   sum:").append(currentCount)
                        .append("---sum:").append(currentReadCount);
                LOGGER.info(sb.toString());
                preCount = currentCount;
                preTime = currentTime;
                preReadCount=currentReadCount;
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        channels.increment();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        channels.decrement();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        readCount.increment();
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
