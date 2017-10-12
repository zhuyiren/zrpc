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

import com.google.common.base.Strings;
import com.zhuyiren.rpc.engine.Engine;
import com.zhuyiren.rpc.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by zhuyiren on 2017/8/3.
 */
public class ZRpcClientBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


    private static final Logger LOGGER= LoggerFactory.getLogger(ZRpcClientBeanDefinitionParser.class);
    private static final String ATTRIBUTE_WORKER_THREAD_COUNT ="workerThreadCount";
    private static final String ATTRIBUTE_USE_ZIP ="useZip";
    private static final String ATTRIBUTE_ZK_CONNECT_URL="zkConnectUrl";
    private static final String ENGINES_ELEMENT="engines";
    private static final String ATTRIBUTE_ZK_NAMESPACE="zkNamespace";
    private static final String ELEMENT_LOAD_BALANCE_TYPE="loadBalanceStrategies";

    private static final String CLIENT_NAME_DEFAULT = "client";

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String workerCountAttr = element.getAttribute(ATTRIBUTE_WORKER_THREAD_COUNT);
        int workCount=0;
        if(!Strings.isNullOrEmpty(workerCountAttr)){
            workCount=Integer.parseInt(workerCountAttr);
        }
        builder.addPropertyValue("workerThreadCount",workCount);
        builder.setDestroyMethodName("shutdown");

        String useZipAttr = element.getAttribute(ATTRIBUTE_USE_ZIP);
        boolean useZip=false;
        if(!Strings.isNullOrEmpty(useZipAttr)){
            try {
                useZip=Boolean.parseBoolean(useZipAttr);
            }catch (Exception e){
                parserContext.getReaderContext().error("The useZip attribute is not boolean type",element);
            }
        }
        builder.addPropertyValue("useZip",useZip);

        String zkConnectUrlAttr = element.getAttribute(ATTRIBUTE_ZK_CONNECT_URL);

        builder.addPropertyValue("zkConnectUrl",zkConnectUrlAttr);

        String zkNamespaceAttr=element.getAttribute(ATTRIBUTE_ZK_NAMESPACE);
        builder.addPropertyValue("zkNamespace",zkNamespaceAttr);

        Set<Class<?>> loadBalanceClasses = CommonUtils.extractClassSet(element, parserContext.getReaderContext(), ELEMENT_LOAD_BALANCE_TYPE);

        builder.addPropertyValue(ELEMENT_LOAD_BALANCE_TYPE,loadBalanceClasses);


    }


    @Override
    protected Class<?> getBeanClass(Element element) {
        return ZRpcClientFactoryBean.class;
    }


    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
        String id = element.getAttribute("id");
        if (Strings.isNullOrEmpty(id)) {
            id=CLIENT_NAME_DEFAULT;
        }
        return id;
    }
}
