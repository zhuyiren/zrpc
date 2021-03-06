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

import com.zhuyiren.rpc.common.DefaultServer;
import com.zhuyiren.rpc.common.Server;
import com.zhuyiren.rpc.engine.Engine;
import org.springframework.beans.factory.SmartFactoryBean;

import java.util.List;

/**
 * @author zhuyiren
 * @date 2017/8/10
 */
public class ZRpcServerFactoryBean implements SmartFactoryBean<Server> {

    private String host;
    private String zkConnectUrl;
    private int port;
    private int ioThreadSize;
    private Server server;
    private boolean useZip;
    private String zkNamespace;
    private List<Engine> engines;


    @Override
    public Server getObject() throws Exception {
        Server server = DefaultServer.newBuilder().host(host)
                .port(port).zkConnect(zkConnectUrl).zkNamespace(zkNamespace).ioThreadSize(ioThreadSize)
                .addEngines(engines)
                .zip(useZip).build();
        this.server = server;
        return server;
    }

    @Override
    public Class<?> getObjectType() {
        return Server.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isPrototype() {
        return false;
    }

    @Override
    public boolean isEagerInit() {
        return true;
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown();
        }
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

    public int getIoThreadSize() {
        return ioThreadSize;
    }

    public void setIoThreadSize(int ioThreadSize) {
        this.ioThreadSize = ioThreadSize;
    }

    public boolean isUseZip() {
        return useZip;
    }

    public void setUseZip(boolean useZip) {
        this.useZip = useZip;
    }


    public String getZkConnectUrl() {
        return zkConnectUrl;
    }

    public void setZkConnectUrl(String zkConnectUrl) {
        this.zkConnectUrl = zkConnectUrl;
    }


    public String getZkNamespace() {
        return zkNamespace;
    }

    public void setZkNamespace(String zkNamespace) {
        this.zkNamespace = zkNamespace;
    }


    public List<Engine> getEngines() {
        return engines;
    }

    public void setEngines(List<Engine> engines) {
        this.engines = engines;
    }
}
