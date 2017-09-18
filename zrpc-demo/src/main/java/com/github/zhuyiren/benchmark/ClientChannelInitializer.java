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

package com.github.zhuyiren.benchmark;

import com.github.zhuyiren.demo.service.StudentService;
import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import com.zhuyiren.rpc.handler.ArgumentHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.ReferenceCountUtil;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * @author zhuyiren
 * @date 2017/9/15
 */
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {


    public static class ClientHandler extends ChannelInboundHandlerAdapter {

        private final Packet packet;
        private final Semaphore semaphore;
        private volatile ChannelHandlerContext context;
        private final int size;
        private final BenchmarkReplay replay;

        public ClientHandler(BenchmarkReplay replay, int size,int bufferSize) {
            this.replay = replay;
            this.size = size;
            semaphore=new Semaphore(bufferSize);
            packet = new Packet();
            packet.setId(1);
            packet.setServiceName(StudentService.class.getCanonicalName());
            packet.setMethodName("getTeacher");
            packet.setType(ProtostuffEngine.PROTOSTUFF_TYPE);
            List<Class> classes = new ArrayList<>();
            classes.add(int.class);
            List<Object> objects = new ArrayList<>();
            objects.add(this.size);
            ProtostuffEngine engine = new ProtostuffEngine();
            ArgumentHolder holder = new ArgumentHolder();
            for (int index = 0; index < objects.size(); index++) {
                holder.addArgument(objects.get(index), classes.get(index));
            }
            try {
                byte[] bytes = engine.encodeArgument(holder);
                packet.setEntity(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            this.context = ctx;
            replay.downLatch.countDown();
        }


        public void send() throws Exception {

            if (semaphore.availablePermits() <= 0) {
                return;
            }
            semaphore.acquire();

            ByteBuf buf = Unpooled.buffer();
            OutputStream outputStream = new ByteBufOutputStream(buf);
            packet.write(outputStream);
            context.writeAndFlush(buf);
        }


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            semaphore.release();
            ReferenceCountUtil.release(msg);
        }
    }

    private final BenchmarkReplay replay;

    public ClientChannelInitializer(BenchmarkReplay replay) {
        this.replay = replay;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4))
                .addLast(new LengthFieldPrepender(4, 0, false))
                .addLast(replay.getClientHandler());
    }
}
