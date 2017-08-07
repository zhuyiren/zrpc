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

package com.zhuyiren.rpc.spring;

import com.zhuyiren.rpc.Client;
import com.zhuyiren.rpc.DefaultClient;
import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.engine.NormalEngine;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuyiren on 2017/8/2.
 */
public class ZRpcClientFactoryBean implements FactoryBean<Client> {


    private Engine[] engines;
    private int workerThreadCount;
    private Client client;

    @Override
    public Client getObject() throws Exception {
        DefaultClient client = new DefaultClient(workerThreadCount);
        if (engines==null || engines.length==0) {
            engines=defaultEngines();
        }
        for (Engine engine : engines) {
            client.registerEngine(engine);
        }
        this.client=client;
        return client;
    }


    public void shutdown(){
        if(client!=null){
            client.shutdown();
        }
    }



    @Override
    public Class<?> getObjectType() {
        return Client.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }


    public Engine[] getEngines() {
        return engines;
    }

    public void setEngines(Engine[] engines) {
        this.engines = engines;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public void setWorkerThreadCount(int workerThreadCount) {
        this.workerThreadCount = workerThreadCount;
    }

    private Engine[] defaultEngines(){
        List<Engine> engines=new ArrayList<>();
        engines.add(new NormalEngine());
        engines.add(new ProtostuffEngine());
        return engines.toArray(new Engine[0]);
    }
}
