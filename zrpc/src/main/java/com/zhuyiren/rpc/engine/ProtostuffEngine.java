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
import com.zhuyiren.rpc.handler.ArgumentHelper;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;

import static com.zhuyiren.rpc.common.CommonConstant.ARGUMENT_HELPER_SCHEMA;
import static com.zhuyiren.rpc.common.CommonConstant.WRAP_RETURN_SCHEMA;

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
    public byte[] encodeArgument(Object[] arguments) throws Exception {
        if (arguments == null) {
            arguments = new Object[]{};
        }
        Class[] classes = new Class[arguments.length];
        for (int index = 0; index < arguments.length; index++) {
            classes[index] = arguments[index].getClass();
        }
        ArgumentHelper argumentHelper = new ArgumentHelper();
        argumentHelper.argumentClasses = classes;
        argumentHelper.arguments = arguments;

        return ProtostuffIOUtil.toByteArray(argumentHelper, ARGUMENT_HELPER_SCHEMA, LinkedBuffer.allocate());
    }

    @Override
    public ArgumentHelper decodeArgument(byte[] inBytes) throws Exception {

        ArgumentHelper result = ARGUMENT_HELPER_SCHEMA.newMessage();
        ProtostuffIOUtil.mergeFrom(inBytes, result, ARGUMENT_HELPER_SCHEMA);
        return result;
    }

    @Override
    public byte[] encodeResult(WrapReturn result) throws Exception {
        byte[] resultBytes = ProtostuffIOUtil.toByteArray(result, WRAP_RETURN_SCHEMA, LinkedBuffer.allocate());
        return resultBytes;
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes) throws Exception {
        WrapReturn wrapReturn = new WrapReturn();
        ProtostuffIOUtil.mergeFrom(inBytes, wrapReturn, WRAP_RETURN_SCHEMA);
        return wrapReturn;
    }
}
