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

package com.github.zhuyiren.demo.programmatic;

import com.github.zhuyiren.demo.service.StudentService;
import com.github.zhuyiren.demo.service.TeacherService;
import com.github.zhuyiren.demo.service.impl.StudentServiceImpl;
import com.zhuyiren.rpc.common.DefaultServer;
import com.zhuyiren.rpc.common.Server;
import com.zhuyiren.rpc.engine.JsonEngine;
import com.zhuyiren.rpc.engine.ProtostuffEngine;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public class TestServer {


    public static void main(String[] args){
        Server server = DefaultServer.newBuilder()
                .port(3324)
                .addEngine(new JsonEngine())
                .addEngine(new ProtostuffEngine())
                .ioThreadSize(8)
                .zip(false)
                .zkConnect("192.168.78.30:2181")
                .zkNamespace("zrpc-demo")
                .host("192.168.78.1").build();
        server.register(StudentService.class.getCanonicalName(),new StudentServiceImpl());
    }


}
