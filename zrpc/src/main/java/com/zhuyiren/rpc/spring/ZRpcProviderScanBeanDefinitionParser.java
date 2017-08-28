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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.core.type.filter.TypeFilter;
import org.w3c.dom.Element;

/**
 * @author zhuyiren
 * @date 2017/8/13
 */
public class ZRpcProviderScanBeanDefinitionParser extends ComponentScanBeanDefinitionParser {

    private static final TypeFilter PROVIDER_TYPE_FILTER = (metadataReader, metadataReaderFactory) -> metadataReader.getAnnotationMetadata().hasAnnotation(ZRpcProvider.class.getCanonicalName())
            && metadataReader.getClassMetadata().isConcrete();

    private static final String ZRPC_PROVIDER_FACTORY_POST_PROCESSOR = ZRpcProviderPostProcessor.class.getCanonicalName();

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        registerProviderPostProcessor(parserContext.getRegistry(), element);
        return super.parse(element, parserContext);

    }

    @Override
    protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(parserContext.getRegistry(), false);
        scanner.addIncludeFilter(PROVIDER_TYPE_FILTER);
        scanner.setResourceLoader(parserContext.getReaderContext().getResourceLoader());
        scanner.setEnvironment(parserContext.getReaderContext().getEnvironment());
        return scanner;
    }


    private void registerProviderPostProcessor(BeanDefinitionRegistry registry, Object source) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(ZRpcProviderPostProcessor.class);
        beanDefinition.setSource(source);
        registry.registerBeanDefinition(ZRPC_PROVIDER_FACTORY_POST_PROCESSOR, beanDefinition);
    }
}
