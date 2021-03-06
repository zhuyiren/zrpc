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


import com.zhuyiren.rpc.common.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

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


    public void writeRequestPacket(Packet request) {
        context.writeAndFlush(request).addListener(future -> {
            if (future.cause() != null) {
                request.setException(future.cause().getMessage());
                resolveResponse(request);
            }
        });
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
    }

    @Override
    public void resolveResponse(Packet response) {
        callHandler.completeCall(response);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        context = ctx;
        callHandler.ready();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Packet response = (Packet) msg;
        resolveResponse(response);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        callHandler.close();
        ctx.fireChannelInactive();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }


}
