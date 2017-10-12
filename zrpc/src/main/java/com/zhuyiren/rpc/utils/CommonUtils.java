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
import com.zhuyiren.rpc.common.ProviderProperty;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zhuyiren.rpc.common.ZRpcPropertiesConstant.ANY_HOST;

/**
 * @author zhuyiren
 * @date 2017/9/3
 */
public final class CommonUtils {

    private static final Pattern PATTERN_PROVIDER_LOAD_BALANCE_INFO=Pattern.compile("^(\\S*):(\\d*):(\\S*):(\\S*)$");


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


    public static Set<Class<?>> extractClassSet(Element parent,XmlReaderContext context,String propertyName){
        Set<Class<?>> result = new HashSet<>();
        List<Element> propertyElements = DomUtils.getChildElementsByTagName(parent, propertyName);
        if(propertyElements.size()>1){
            context.error("must only have one ["+propertyName+"] element",parent);
        }
        if(propertyElements.isEmpty()){
            return result;
        }
        List<Element> listElements = DomUtils.getChildElementsByTagName(propertyElements.get(0), "set");
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

    public static ProviderProperty parseLoadBalanceConfig(String config){
        Matcher matcher = PATTERN_PROVIDER_LOAD_BALANCE_INFO.matcher(config);
        if(!matcher.find()){
            throw new IllegalArgumentException("The property provider information is not hte pattern [host:ip:loadBalanceType:loadBalanceProperty]");
        }
        String host=matcher.group(1);
        String port = matcher.group(2);
        String loadBalanceType=matcher.group(3);
        String loadBalanceProperty = matcher.group(4);

        SocketAddress address=null;
        if(!Strings.isNullOrEmpty(host) && !Strings.isNullOrEmpty(port)){
            address=new InetSocketAddress(host,Integer.parseInt(port));
        }
        return new ProviderProperty(address, loadBalanceType, loadBalanceProperty);
    }

}
