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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author zhuyiren
 * @date 2017/8/10
 */
public class ZRpcServerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


    private static final Logger LOGGER= LoggerFactory.getLogger(ZRpcServerBeanDefinitionParser.class);

    private static final String HOST_ATTRIBUTE ="host";
    private static final String PORT_ATTRIBUTE ="port";
    private static final String ATTRIBUTE_IO_THREAD_SIZE ="ioThreadSize";
    private static final String ATTRIBUTE_USE_ZIP ="useZip";
    private static final String ATTRIBUTE_ZK_CONNECT_URL="zkConnectUrl";
    private static final String SERVER_NAME_DEFAULT="server";
    private static final String ATTRIBUTE_ZK_NAMESPACE="zkNamespace";



    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String host = element.getAttribute(HOST_ATTRIBUTE);
        String attrPort = element.getAttribute(PORT_ATTRIBUTE);
        int port=0;
        if(StringUtils.hasText(attrPort)){
            try {
                port=Integer.parseInt(attrPort);
            }catch (Exception e){
                LOGGER.warn("The server port is not a number,and will set be default");
            }
        }
        int ioThreadSize=0;
        String attrIoThreadSize = element.getAttribute(ATTRIBUTE_IO_THREAD_SIZE);
        if(StringUtils.hasText(attrIoThreadSize)){
            try {
                ioThreadSize=Integer.parseInt(attrIoThreadSize);
            }catch (Exception e){
                LOGGER.warn("The io thread size is not a number,and will set be default");
            }
        }


        String useZipAttr=element.getAttribute(ATTRIBUTE_USE_ZIP);
        boolean useZip=false;
        try {
            useZip=Boolean.parseBoolean(useZipAttr);
        }catch (Exception e){
            LOGGER.error("The useZip attribute is not boolean type");
        }
        builder.addPropertyValue("useZip",useZip);

        String zkConnectUrlAttr = element.getAttribute(ATTRIBUTE_ZK_CONNECT_URL);
        if(!StringUtils.hasText(zkConnectUrlAttr)){
            parserContext.getReaderContext().error("The Zookeeper connect's url must be set",element);
        }

        String zkNamespaceAttr=element.getAttribute(ATTRIBUTE_ZK_NAMESPACE);


        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        beanDefinition.getPropertyValues().addPropertyValue("host",host);
        beanDefinition.getPropertyValues().addPropertyValue("zkNamespace",zkNamespaceAttr);
        beanDefinition.getPropertyValues().addPropertyValue("port",port);
        beanDefinition.getPropertyValues().addPropertyValue("ioThreadSize",ioThreadSize);
        builder.addPropertyValue("zkConnectUrl",zkConnectUrlAttr);
        beanDefinition.setDestroyMethodName("shutdown");
    }

    protected Class<?> getBeanClass(Element element) {
        return ZRpcServerFactoryBean.class;
    }


    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
        String id = element.getAttribute("id");
        if (!StringUtils.hasText(id)) {
            id=SERVER_NAME_DEFAULT;
        }
        return id;
    }
}
