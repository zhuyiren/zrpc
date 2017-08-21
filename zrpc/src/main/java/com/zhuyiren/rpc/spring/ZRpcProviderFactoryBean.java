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

import com.zhuyiren.rpc.common.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author zhuyiren
 * @date 2017/8/10
 */
public class  ZRpcProviderFactoryBean implements SmartFactoryBean,ApplicationContextAware {

    private static final Logger LOGGER= LoggerFactory.getLogger(ZRpcProviderFactoryBean.class);

    private String serviceName;
    private String host;
    private int port;
    private Object handler;
    private Server server;
    private ApplicationContext context;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context=applicationContext;
    }

    @Override
    public Object getObject() throws Exception {
        if(server==null){
            server=context.getBean(Server.class);
            if(server==null){
                throw new IllegalStateException("The server is not set and don't find from the application");
            }else {
                LOGGER.warn("The server is not set,and will find from application");
            }
        }
        if(handler==null){
            throw new IllegalStateException("The handler is not set");
        }
        if(!StringUtils.hasText(serviceName)){
            Class<?>[] allInterfaces = ClassUtils.getAllInterfaces(handler);
            if(allInterfaces.length==1){
                serviceName=allInterfaces[0].getCanonicalName();
            }else {
                throw new IllegalStateException("Can't determine the service name");
            }
        }
        server.register(serviceName,handler,host,port);
        return handler;
    }

    @Override
    public Class<?> getObjectType() {
        return ZRpcProviderFactoryBean.class;
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public Object getHandler() {
        return handler;
    }

    public void setHandler(Object handler) {
        this.handler = handler;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
