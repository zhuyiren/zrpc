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

import com.github.zhuyiren.demo.model.TeacherInfo;
import com.github.zhuyiren.demo.service.StudentService;
import com.zhuyiren.rpc.common.DefaultClient;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import com.zhuyiren.rpc.loadbalance.RandomLoadBalanceStrategy;
import com.zhuyiren.rpc.loadbalance.RoundRobinLoadBalanceStrategy;
import com.zhuyiren.rpc.loadbalance.WeightedRoundRobinLoadBalanceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public class TestClient {

    private static final Logger LOGGER= LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args)throws Exception {
        DefaultClient client = new DefaultClient("192.168.78.30:2181", "zrpc-demo", 4, false);
        client.registerLoadBalance(WeightedRoundRobinLoadBalanceStrategy.class);
        client.registerLoadBalance(RandomLoadBalanceStrategy.class);
        client.registerLoadBalance(RoundRobinLoadBalanceStrategy.class);
        StudentService studentService = client.exportService(ProtostuffEngine.class, StudentService.class,null);
        while (true){
            try {
                TeacherInfo teacher = studentService.getTeacher(3);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(),e);
                Thread.sleep(1000);
            }
        }
    }
}
