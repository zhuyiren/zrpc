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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhuyiren on 2017/8/7.
 */
public class ServerIdleHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER= LoggerFactory.getLogger(ServerIdleHandler.class);

    private static final Packet PACKET_PONG;

    static {
        PACKET_PONG=new Packet();
        PACKET_PONG.setType(CommonConstant.IDLE_PONG);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if(msg instanceof Packet){
            if (CommonConstant.IDLE_PING.equals(((Packet) msg).getType())) {
                ctx.writeAndFlush(PACKET_PONG);
                LOGGER.debug("receive a heartbeat");
                return;
            }
        }
        ctx.fireChannelRead(msg);
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
