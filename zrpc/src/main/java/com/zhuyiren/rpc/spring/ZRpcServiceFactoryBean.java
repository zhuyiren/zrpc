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
import com.zhuyiren.rpc.engine.Engine;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.InetSocketAddress;

/**
 * Created by zhuyiren on 2017/8/2.
 */
public class ZRpcServiceFactoryBean implements FactoryBean,ApplicationContextAware{


    private Class<?> ifcCls;
    private Client client;
    private Class<? extends Engine> engine;
    private String host;
    private int port;
    private boolean cache;
    private String serviceName;

    private ApplicationContext context;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context=applicationContext;
    }

    @Override
    public Object getObject() throws Exception {
        InetSocketAddress address = new InetSocketAddress(host, port);
        if(client==null){
            this.client=context.getBean(Client.class);
        }
        return client.exportService(engine, ifcCls,serviceName, address, cache);
    }

    @Override
    public Class<?> getObjectType() {
        return ifcCls;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }


    public Class<?> getIfcCls() {
        return ifcCls;
    }

    public void setIfcCls(Class<?> ifcCls) {
        this.ifcCls = ifcCls;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Class<? extends Engine> getEngine() {
        return engine;
    }

    public void setEngine(Class<? extends Engine> engine) {
        this.engine = engine;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
