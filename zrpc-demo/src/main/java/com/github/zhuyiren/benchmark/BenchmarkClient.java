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

import com.github.zhuyiren.demo.model.TeacherInfo;
import com.github.zhuyiren.demo.service.StudentService;
import com.github.zhuyiren.demo.service.impl.StudentServiceImpl;
import com.google.common.base.Preconditions;
import com.zhuyiren.rpc.common.DefaultClient;
import com.zhuyiren.rpc.common.ProviderProperty;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import com.zhuyiren.rpc.loadbalance.RandomLoadBalanceStrategy;
import com.zhuyiren.rpc.loadbalance.RoundRobinLoadBalanceStrategy;
import com.zhuyiren.rpc.loadbalance.WeightedRoundRobinLoadBalanceStrategy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhuyiren
 * @date 2017/9/15
 */
public class BenchmarkClient {


    private String host;
    private int channelSize;
    private int serialSize;
    private int sendThread;


    private static class SendTask implements Callable<Integer>{
        private final StudentService[] services;
        private final int serialSize;
        public SendTask(StudentService[] services,int serialSize){
            this.serialSize=serialSize;
            this.services=services;
        }
        @Override
        public Integer call() throws Exception {
            long times=0;
            StudentService localService=new StudentServiceImpl();
            TeacherInfo localTeacher = localService.getTeacher(serialSize);
            while (true){
                try {
                    TeacherInfo teacher = services[(int) (times++ % services.length)].getTeacher(serialSize);
                    if(!localTeacher.equals(teacher)){
                        System.out.println("not equals");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Thread.sleep(1000);
                }
            }
        }
    }

    private void start() throws Exception{
        StudentService[] studentServices=new StudentService[channelSize];
        DefaultClient temp=new DefaultClient();
        for (int index = 0; index < studentServices.length; index++) {
            DefaultClient client=new DefaultClient();
            client.getEventExecutors().shutdownGracefully();
            client.setEventExecutors(temp.getEventExecutors());
            client.registerLoadBalance(WeightedRoundRobinLoadBalanceStrategy.class);
            client.registerLoadBalance(RandomLoadBalanceStrategy.class);
            client.registerLoadBalance(RoundRobinLoadBalanceStrategy.class);
            studentServices[index]=client.exportService(ProtostuffEngine.class,StudentService.class,
                    Arrays.asList(new ProviderProperty(new InetSocketAddress(host,3324),WeightedRoundRobinLoadBalanceStrategy.LOAD_BALANCE_TYPE,"1")));
        }

        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Callable<Integer>> tasks=new ArrayList<>();
        for (int index = 0; index < sendThread; index++) {
            tasks.add(new SendTask(studentServices, serialSize));
        }
        executorService.invokeAll(tasks);
    }

    public BenchmarkClient(String host,int channelSize,int serialSize,int sendThread){
        this.host=host;
        this.channelSize=channelSize;
        this.serialSize=serialSize;
        this.sendThread=sendThread;
    }

    public static void main(String[] args) throws Exception{
        Preconditions.checkElementIndex(3,args.length);
        String host=args[0];
        int channelSize=Integer.parseInt(args[1]);
        int serialSize=Integer.parseInt(args[2]);
        int sendThread=Integer.parseInt(args[3]);
        BenchmarkClient benchmark = new BenchmarkClient(host, channelSize, serialSize,sendThread);
        benchmark.start();
    }
}
