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

import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHolder;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;

import java.lang.reflect.Type;

import static com.zhuyiren.rpc.common.ZRpcPropertiesConstant.ARGUMENT_HELPER_SCHEMA;
import static com.zhuyiren.rpc.common.ZRpcPropertiesConstant.WRAP_RETURN_SCHEMA;

/**
 * Created by zhuyiren on 2017/6/4.
 */
public class ProtostuffEngine extends AbstractEngine implements Engine {


    public static final String PROTOSTUFF_TYPE = "protostuff";


    @Override
    public String getType() {
        return PROTOSTUFF_TYPE;
    }


    @Override
    public byte[] encodeArgument(ArgumentHolder argumentHolder) throws Exception {
        return ProtostuffIOUtil.toByteArray(argumentHolder, ARGUMENT_HELPER_SCHEMA, LinkedBuffer.allocate());
    }

    @Override
    public ArgumentHolder decodeArgument(byte[] inBytes) throws Exception {
        ArgumentHolder result = new ArgumentHolder();
        ProtostuffIOUtil.mergeFrom(inBytes, result, ARGUMENT_HELPER_SCHEMA);
        return result;
    }

    @Override
    public byte[] encodeResult(WrapReturn result) throws Exception {
        return ProtostuffIOUtil.toByteArray(result, WRAP_RETURN_SCHEMA, LinkedBuffer.allocate());
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes, Type type) throws Exception {
        WrapReturn wrapReturn = new WrapReturn();
        ProtostuffIOUtil.mergeFrom(inBytes, wrapReturn, WRAP_RETURN_SCHEMA);
        return wrapReturn;
    }
}
