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

package com.zhuyiren.rpc.utils;

import com.google.common.base.Strings;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import static com.zhuyiren.rpc.common.ZRpcPropertiesConstant.ANY_HOST;

/**
 * @author zhuyiren
 * @date 2017/9/3
 */
public final class CommonUtils {


    private CommonUtils() {

    }

    public static void checkNoAnyHost(List<SocketAddress> addresses) {
        addresses.forEach(address -> {
            if (address instanceof InetSocketAddress) {
                if (ANY_HOST.equals(((InetSocketAddress) address).getAddress().getHostAddress())) {
                    throw new IllegalArgumentException("The address must not be 0.0.0.0");
                }
            }
        });
    }

    public static List<Class<?>> extractClassList(Element parent, XmlReaderContext context,String propertyName) {
        List<Class<?>> result = new ArrayList<>();
        List<Element> propertyElements = DomUtils.getChildElementsByTagName(parent, propertyName);
        if(propertyElements.size()>1){
            context.error("must only have one ["+propertyName+"] element",parent);
        }
        if(propertyElements.isEmpty()){
            return result;
        }
        List<Element> listElements = DomUtils.getChildElementsByTagName(propertyElements.get(0), "list");
        if(listElements.size()>1){
            context.error("must not have more than one list element",parent);
        }
        Element listElement = listElements.get(0);
        BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(context);
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();
        List<Object> objects = delegate.parseListElement(listElement, rootBeanDefinition);
        for (Object object : objects) {
            if (object instanceof TypedStringValue && !Strings.isNullOrEmpty(((TypedStringValue) object).getValue())) {
                try {
                    Class<?> targetClass = Class.forName(((TypedStringValue) object).getValue());
                    result.add(targetClass);
                } catch (ClassNotFoundException e) {
                    context.error(e.getMessage(), listElement);
                } catch (Exception e) {
                    context.error(e.getMessage(), listElement);
                }
            }
        }
        return result;
    }
}
