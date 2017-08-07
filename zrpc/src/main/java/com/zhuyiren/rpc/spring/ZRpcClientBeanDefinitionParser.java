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
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuyiren on 2017/8/3.
 */
public class ZRpcClientBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


    private static final Logger LOGGER= LoggerFactory.getLogger(ZRpcClientBeanDefinitionParser.class);
    private static final String WORKERTHREADCOUNT_ATTRIBUTE="workerThreadCount";
    private static final String ENGINES_ELEMENT="engines";
    private static final String LIST_ELEMENT="list";

    private static final String CLIENT_NAME_DEFAULT = "client";

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        BeanDefinitionParserDelegate delegate=new BeanDefinitionParserDelegate(parserContext.getReaderContext());

        String workerCountAttr = element.getAttribute(WORKERTHREADCOUNT_ATTRIBUTE);
        int workCount=0;
        if(StringUtils.hasText(workerCountAttr)){
            workCount=Integer.parseInt(workerCountAttr);
        }
        builder.addPropertyValue("workerThreadCount",workCount);
        List<Element> enginesElements = DomUtils.getChildElementsByTagName(element, ENGINES_ELEMENT);
        if(enginesElements.size()>1){
            parserContext.getReaderContext().error("must have one engines element",element);
        }
        Element enginesElement = enginesElements.get(0);
        List<Element> listElements = DomUtils.getChildElementsByTagName(enginesElement, "list");
        if(listElements.size()>1){
            parserContext.getReaderContext().error("must have one list element",element);
        }
        Element listElement = listElements.get(0);
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();
        List<Object> objects = delegate.parseListElement(listElement, rootBeanDefinition);
        List<Engine> engines=new ArrayList<>();
        for (Object object : objects) {
            if(object instanceof TypedStringValue && StringUtils.hasText(((TypedStringValue) object).getValue())){
                try {
                    Class<? extends Engine> engineCls = (Class<? extends Engine>) Class.forName(((TypedStringValue) object).getValue());
                    Engine engine = engineCls.newInstance();
                    if(engines.contains(engine)){
                       if(LOGGER.isDebugEnabled()){
                           LOGGER.debug(engineCls +" have registed the Client once");
                           continue;
                       }
                    }
                    engines.add(engine);
                } catch (ClassNotFoundException e) {
                    parserContext.getReaderContext().error(((TypedStringValue) object).getValue()+
                    " is not a implementation of Engine interface",listElement);
                }catch (Exception e){
                    parserContext.getReaderContext().error(e.getMessage(),listElement);
                }
            }
        }
        builder.addPropertyValue("engines",engines);

    }


    @Override
    protected Class<?> getBeanClass(Element element) {
        return ZRpcClientFactoryBean.class;
    }


    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
        String id = element.getAttribute("id");
        if (!StringUtils.hasText(id)) {
            id=CLIENT_NAME_DEFAULT;
        }
        return id;
    }
}
