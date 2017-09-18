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

import com.google.common.base.Preconditions;
import com.zhuyiren.rpc.common.Client;
import com.zhuyiren.rpc.common.DefaultClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author zhuyiren
 * @date 2017/9/15
 */
public class BenchmarkReplay {


    private final List<ClientChannelInitializer.ClientHandler> handlers=new CopyOnWriteArrayList<>();
    public  final CountDownLatch downLatch;
    public  final int serialSize;
    private final int channelSize;
    private final String host;
    private final int bufferSize;
    private final int sendThreadSize;

    public BenchmarkReplay(String host,int channelSize,int serialSize,int bufferSize,int sendThreadSize){
        this.host=host;
        this.channelSize=channelSize;
        this.serialSize=serialSize;
        this.bufferSize=bufferSize;
        this.sendThreadSize=sendThreadSize;
        downLatch=new CountDownLatch(channelSize);
    }

    public static void main(String[] args) throws Exception{
        Preconditions.checkElementIndex(4,args.length);
        String host=args[0];
        int channelSize=Integer.parseInt(args[1]);
        int serialSize=Integer.parseInt(args[2]);
        int bufferSize=Integer.parseInt(args[3]);
        int sendThreadSize=Integer.parseInt(args[4]);
        BenchmarkReplay replay = new BenchmarkReplay(host,channelSize, serialSize,bufferSize,sendThreadSize);
        replay.start();
    }

    public ClientChannelInitializer.ClientHandler getClientHandler() {
        ClientChannelInitializer.ClientHandler handler = new ClientChannelInitializer.ClientHandler(this,serialSize,bufferSize);
        handlers.add(handler);
        System.out.println("create handler");
        return handler;
    }

    public void start() throws Exception{
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(4);
        ClientChannelInitializer initializer=new ClientChannelInitializer(this);
        bootstrap.group(group).channel(NioSocketChannel.class).handler(initializer);
        for (int index = 0; index < channelSize; index++) {
            bootstrap.connect(host,3324).sync();
        }

        ExecutorService executorService = Executors.newCachedThreadPool();

        for (int index = 0; index < sendThreadSize; index++) {
            executorService.submit((Callable<Integer>) () -> {
                downLatch.await();
                long times=0;
                System.out.println("begin replay");
                while (true){
                    handlers.get((int)(times++%handlers.size())).send();
                }
            });
        }
    }
}
