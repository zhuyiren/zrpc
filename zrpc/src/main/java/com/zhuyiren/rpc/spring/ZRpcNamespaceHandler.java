package com.zhuyiren.rpc.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Created by zhuyiren on 2017/6/28.
 */
public class ZRpcNamespaceHandler extends NamespaceHandlerSupport {


    @Override
    public void init() {
        registerBeanDefinitionParser("client",new ZRpcClientBeanDefinitionParser());
        registerBeanDefinitionParser("service",new ZRpcServiceBeanDefinitionParser());
    }
}
