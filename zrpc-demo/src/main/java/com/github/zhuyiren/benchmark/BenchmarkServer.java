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
import com.github.zhuyiren.demo.service.impl.StudentServiceImpl;
import com.google.common.base.Preconditions;
import com.zhuyiren.rpc.common.DefaultServer;
import com.zhuyiren.rpc.common.Server;
import com.zhuyiren.rpc.engine.JsonEngine;
import com.zhuyiren.rpc.engine.ProtostuffEngine;

/**
 * @author zhuyiren
 * @date 2017/9/15
 */
public class BenchmarkServer {


    private String host;

    private void start(){
        Server server = DefaultServer.newBuilder()
                .port(3324)
                .addEngine(new JsonEngine())
                .addEngine(new ProtostuffEngine())
                .zip(false)
                .host(host).build();
        server.register(StudentService.class.getCanonicalName(),new StudentServiceImpl());
    }

    public BenchmarkServer(String host){
        this.host=host;
    }

    public static void main(String[] args) throws Exception{
        Preconditions.checkElementIndex(0,args.length);
        String host=args[0];
        BenchmarkServer benchmark = new BenchmarkServer(host);
        benchmark.start();
    }
}
