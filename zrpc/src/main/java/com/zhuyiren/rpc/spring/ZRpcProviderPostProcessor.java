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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * @author zhuyiren
 * @date 2017/8/13
 */
public class ZRpcProviderPostProcessor implements BeanFactoryPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZRpcProviderPostProcessor.class);

    private static final String ATTRIBUTE_SERVICE_NAME = "serviceName";
    private static final String ATTRIBUTE_HOST = "host";
    private static final String ATTRIBUTE_PORT = "port";
    private static final String ATTRIBUTE_SERVER = "server";
    private static final String ATTRIBUTE_LOAD_BALANCE_TYPE = "loadBalanceType";

    private BeanNameGenerator nameGenerator = new DefaultBeanNameGenerator();

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof BeanDefinitionRegistry)) {
            return;
        }
        String[] names = beanFactory.getBeanDefinitionNames();
        for (String beanName : names) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition instanceof AnnotatedBeanDefinition
                    && ((AnnotatedBeanDefinition) beanDefinition).getMetadata().getAnnotationTypes().contains(ZRpcProvider.class.getCanonicalName())) {
                try {
                    registerProviderFactoryBeanDefinition(new BeanDefinitionHolder(beanDefinition, beanName), ((BeanDefinitionRegistry) beanFactory));
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Register provider class occur error", e);
                }
            }
        }
    }


    /**
     * @param beanDefinitionHolder
     * @param registry
     */
    private void registerProviderFactoryBeanDefinition(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry) throws ClassNotFoundException {
        AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) beanDefinitionHolder.getBeanDefinition();

        RootBeanDefinition providerBeanDefinition = new RootBeanDefinition();
        providerBeanDefinition.setBeanClass(ZRpcProviderFactoryBean.class);
        providerBeanDefinition.setLazyInit(false);
        Map<String, Object> attributes = beanDefinition.getMetadata()
                .getAnnotationAttributes(ZRpcProvider.class.getCanonicalName());
        String attrServiceName = (String) attributes.get(ATTRIBUTE_SERVICE_NAME);
        if (Strings.isNullOrEmpty(attrServiceName)) {
            attrServiceName = getServiceName(beanDefinition);
        }
        providerBeanDefinition.getPropertyValues().addPropertyValue("serviceName", attrServiceName);

        String attrHost = (String) attributes.get(ATTRIBUTE_HOST);
        providerBeanDefinition.getPropertyValues().addPropertyValue("host", attrHost);


        int attrPort = (int) attributes.get(ATTRIBUTE_PORT);
        providerBeanDefinition.getPropertyValues().addPropertyValue("port", attrPort);

        String attrServer = (String) attributes.get(ATTRIBUTE_SERVER);
        if (!Strings.isNullOrEmpty(attrServer)) {
            RuntimeBeanReference serverReference = new RuntimeBeanReference(attrServer);
            providerBeanDefinition.getPropertyValues().addPropertyValue("server", serverReference);
        }

        RuntimeBeanReference handlerReference = new RuntimeBeanReference(beanDefinitionHolder.getBeanName());
        providerBeanDefinition.getPropertyValues().addPropertyValue("handler", handlerReference);

        String loadBalanceType = (String) attributes.get(ATTRIBUTE_LOAD_BALANCE_TYPE);
        providerBeanDefinition.getPropertyValues().addPropertyValue("loadBalanceType", loadBalanceType);
        registry.registerBeanDefinition(nameGenerator.generateBeanName(providerBeanDefinition, registry), providerBeanDefinition);
    }


    private String getServiceName(BeanDefinition beanDefinition) throws ClassNotFoundException {
        String beanClassName = beanDefinition.getBeanClassName();
        Class<?> clz = Class.forName(beanClassName);
        Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(clz);
        if (interfaces.length == 1) {
            return interfaces[0].getCanonicalName();
        } else {
            return beanClassName;
        }
    }
}
