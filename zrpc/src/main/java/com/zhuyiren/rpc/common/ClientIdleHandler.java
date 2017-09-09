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


package com.zhuyiren.rpc.common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhuyiren on 2017/8/7.
 */
public class ClientIdleHandler extends ChannelInboundHandlerAdapter {


    private ScheduledFuture<?> future;

    private static final Packet PING_PACKET;

    private static final long PERIOD = 5000;


    static {
        PING_PACKET = new Packet();
        PING_PACKET.setId(-1);
        PING_PACKET.setType(ZRpcPropertiesConstant.IDLE_PING);
    }

    public ClientIdleHandler() {
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        future = ctx.executor().scheduleWithFixedDelay(() ->
                        ctx.writeAndFlush(PING_PACKET)
                , PERIOD, PERIOD, TimeUnit.MILLISECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (future != null) {
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
