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
import com.zhuyiren.rpc.common.ProviderProperty;
import com.zhuyiren.rpc.utils.CommonUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author zhuyiren
 * @date 2017/8/14
 */
public class ZRpcServiceScanBeanDefinitionParser implements BeanDefinitionParser {


    private static final String ATTRIBUTE_BASE_PACKAGE = "base-package";
    private static final String ATTRIBUTE_PROVIDERS="providers";
    private static final BeanNameGenerator NAME_GENERATOR = new AnnotationBeanNameGenerator();
    private static final Pattern PATTERN_PROVIDER_LOAD_BALANCE_INFO=Pattern.compile("^(\\S*):(\\d*):(\\S*):(\\S*)$");



    private static final TypeFilter SERVICE_TYPE_FILTER = (metadataReader, metadataReaderFactory) -> metadataReader.getAnnotationMetadata().hasAnnotation(ZRpcService.class.getCanonicalName())
            && metadataReader.getClassMetadata().isInterface();


    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String basePackage = element.getAttribute(ATTRIBUTE_BASE_PACKAGE);
        basePackage = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(basePackage);
        String[] basePackages = StringUtils.tokenizeToStringArray(basePackage,
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);


        ClassPathScanningCandidateComponentProvider scanner = configureScanner(parserContext);
        try {
            parseAndRegister(parserContext, scanner, basePackages, element);
        } catch (Exception e) {
            parserContext.getReaderContext().error(e.getMessage(), element);
        }
        return null;
    }


    private ClassPathScanningCandidateComponentProvider configureScanner(ParserContext parserContext) {
        ZRpcClassScanner scanner = new ZRpcClassScanner();
        scanner.addIncludeFilter(SERVICE_TYPE_FILTER);
        scanner.setResourceLoader(parserContext.getReaderContext().getResourceLoader());
        scanner.setEnvironment(parserContext.getReaderContext().getEnvironment());
        return scanner;
    }

    private void parseAndRegister(ParserContext parserContext, ClassPathScanningCandidateComponentProvider scanner, String[] basePackages, Object source) throws Exception {
        for (String packageName : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(packageName);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    String beanName = NAME_GENERATOR.generateBeanName(candidateComponent, parserContext.getRegistry());
                    BeanDefinition beanDefinition = parseAttributes(((AnnotatedBeanDefinition) candidateComponent));
                    parserContext.getRegistry().registerBeanDefinition(beanName, beanDefinition);
                }
            }
        }
    }


    private BeanDefinition parseAttributes(AnnotatedBeanDefinition beanDefinition) throws Exception {
        Map<String, Object> attributes = beanDefinition.getMetadata().getAnnotationAttributes(ZRpcService.class.getCanonicalName());
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();
        rootBeanDefinition.setLazyInit(false);
        rootBeanDefinition.setBeanClass(ZRpcServiceFactoryBean.class);
        Class<?> attrIfcCls =  Class.forName(beanDefinition.getBeanClassName());
        rootBeanDefinition.getPropertyValues().addPropertyValue("ifcCls", attrIfcCls);

        String attrClient = (String) attributes.get("client");
        if (!Strings.isNullOrEmpty(attrClient)) {
            RuntimeBeanReference clientReference = new RuntimeBeanReference(attrClient);
            rootBeanDefinition.getPropertyValues().addPropertyValue("client", clientReference);
        }

        Object engine = attributes.get("engine");
        rootBeanDefinition.getPropertyValues().addPropertyValue("engine", engine);


        String serviceName = ((String) attributes.get("serviceName"));
        if (Strings.isNullOrEmpty(serviceName)) {
            serviceName = attrIfcCls.getCanonicalName();
        }
        rootBeanDefinition.getPropertyValues().addPropertyValue("serviceName", serviceName);

        String[] attrProviders = (String[]) attributes.get(ATTRIBUTE_PROVIDERS);

        List<ProviderProperty> providers=new ArrayList<>();
        for (String attrProvider : attrProviders) {
            if(Strings.isNullOrEmpty(attrProvider)){
                continue;
            }
            ProviderProperty provider = CommonUtils.parseLoadBalanceConfig(attrProvider);
            providers.add(provider);
        }
        rootBeanDefinition.getPropertyValues().addPropertyValue("providers",providers);

        return rootBeanDefinition;
    }
}
