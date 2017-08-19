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

package com.zhuyiren.rpc.engine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.ParameterizedTypeImpl;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHolder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhuyiren
 * @date 2017/8/17
 */
public class JsonEngine  extends AbstractEngine{

    private static final String ENGINE_JSON="json";
    private Map<Type,ParameterizedType> typeMap;

    public JsonEngine(){
        typeMap=new HashMap<>();
    }

    @Override
    public String getType() {
        return ENGINE_JSON;
    }

    @Override
    public byte[] encodeArgument(ArgumentHolder argumentHolder) throws Exception {
        return JSON.toJSONBytes(argumentHolder);
    }

    @Override
    public ArgumentHolder decodeArgument(byte[] inBytes) throws Exception {
        return JSON.parseObject(inBytes, ArgumentHolder.class);
    }

    @Override
    public byte[] encodeResult(WrapReturn wrapReturn) throws Exception {
        return JSON.toJSONBytes(wrapReturn);
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes, Type type) throws Exception {
        ParameterizedType parameterizedType = typeMap.computeIfAbsent(type, key -> new ParameterizedTypeImpl(new Type[]{key}, null, WrapReturn.class));
        return JSON.parseObject(inBytes,parameterizedType);
    }
}
