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
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by zhuyiren on 2017/8/3.
 */
public class ZRpcServiceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


    private static final String ATTRIBUTE_CLASS = "interface";
    private static final String ATTRIBUTE_ENGINE = "engineType";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_HOST = "host";
    private static final String ATTRIBUTE_SERVICE_NAME = "serviceName";
    private static final String ATTRIBUTE_PORT = "port";

    private static final String ATTRIBUTE_REF="ref";


    private static final int PORT_DEFAULT = 3324;




    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

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


        String serviceNameAttr = element.getAttribute(ATTRIBUTE_SERVICE_NAME);
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

        Object clientRef=null;

        NodeList childNodes = element.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node item = childNodes.item(index);
            if(item instanceof Element &&
                    item.getLocalName().equals("clientRef") &&
                    ((Element) item).hasAttribute(ATTRIBUTE_REF)){
                clientRef= parseRef(parserContext,((Element) item));
                break;
            }
        }

        if(clientRef==null){
            clientRef=new RuntimeBeanReference("client");
        }

        builder.addPropertyValue("client",clientRef);
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

    private Object parseRef(ParserContext parserContext,Element element){
        String attribute = element.getAttribute(ATTRIBUTE_REF);
        RuntimeBeanReference reference = new RuntimeBeanReference(attribute);
        reference.setSource(parserContext.extractSource(element));
        return reference;
    }

}
