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

import com.google.protobuf.MessageLite;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHelper;

/**
 * Created by zhuyiren on 2017/6/29.
 */
public class ProtobufEngine extends AbstractEngine {


    public static final String ENGINE_PROTOBUF="pb";


    @Override
    public String getType() {
        return ENGINE_PROTOBUF;
    }




    @Override
    public byte[] encodeArgument(Object[] arguments) throws Exception {
        return ((MessageLite) arguments[0]).toByteArray();
    }

    @Override
    public ArgumentHelper decodeArgument(byte[] inBytes) throws Exception {
        return null;
    }

    @Override
    public byte[] encodeResult(WrapReturn result) throws Exception {
        return new byte[0];
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes) throws Exception {
        return null;
    }
}
