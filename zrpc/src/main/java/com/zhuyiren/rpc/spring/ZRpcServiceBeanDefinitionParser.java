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

import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Created by zhuyiren on 2017/8/3.
 */
public class ZRpcServiceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


    private static final String ATTRIBUTE_CLASS = "interface";
    private static final String ATTRIBUTE_ENGINE = "engineType";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_HOST = "host";
    private static final String ATTRIBUTE_CACHED = "cached";
    private static final String ATTRIBUTE_SERVICENAME = "serviceName";
    private static final String ATTRIBUTE_PORT = "port";


    private static final int PORT_DEFAULT = 3324;


    private static final ClassLoader CLASS_LOADER = Thread.currentThread().getContextClassLoader();


    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(parserContext.getReaderContext());

        String ifsClzAttr = element.getAttribute(ATTRIBUTE_CLASS);
        Class<?> ifsClzType = null;
        try {
            ifsClzType = Class.forName(ifsClzAttr);
        } catch (ClassNotFoundException e) {
            parserContext.getReaderContext().error(e.getMessage(), element);
        }
        builder.addPropertyValue("ifcCls", ifsClzType);

        String host = element.getAttribute(ATTRIBUTE_HOST);
        builder.addPropertyValue("host", host);


        String engineAttr = element.getAttribute(ATTRIBUTE_ENGINE);
        Class<? extends Engine> engineCls = ProtostuffEngine.class;
        if (!StringUtils.isEmpty(engineAttr)) {
            try {
                engineCls = (Class<? extends Engine>) Class.forName(engineAttr);
            } catch (ClassNotFoundException e) {
                parserContext.getReaderContext().error(ATTRIBUTE_ENGINE + " must be a implementation of com.zhuyiren.rpc.engine.Engine", element);
            }
        }
        builder.addPropertyValue("engine", engineCls);

        String cachedAttr = element.getAttribute(ATTRIBUTE_CACHED);
        boolean cached = false;
        if (!StringUtils.isEmpty(cachedAttr)) {
            cached = Boolean.parseBoolean(cachedAttr);
        }
        builder.addPropertyValue("cache", cached);

        String serviceNameAttr = element.getAttribute(ATTRIBUTE_SERVICENAME);
        if (StringUtils.isEmpty(serviceNameAttr)) {
            serviceNameAttr = ifsClzAttr;
        }
        builder.addPropertyValue("serviceName", serviceNameAttr);

        String portAttr = element.getAttribute(ATTRIBUTE_PORT);
        int port = PORT_DEFAULT;
        if (!StringUtils.isEmpty(portAttr)) {
            port = Integer.parseInt(portAttr);
        }
        builder.addPropertyValue("port", port);


        delegate.parsePropertyElements(element, builder.getBeanDefinition());
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return ZRpcServiceFactoryBean.class;

    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
        String id = element.getAttribute(ATTRIBUTE_ID);
        if (!StringUtils.hasText(id)) {
            id = parserContext.getReaderContext().generateBeanName(definition);
        }
        return id;
    }
}
