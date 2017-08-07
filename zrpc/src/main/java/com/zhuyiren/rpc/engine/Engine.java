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


import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHelper;
import com.zhuyiren.rpc.handler.Invoker;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketAddress;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public interface Engine {


    String getType();



    byte[] encodeArgument(Object[] arguments) throws Exception;

    ArgumentHelper decodeArgument(byte[] inBytes) throws Exception;


    byte[] encodeResult(WrapReturn result) throws Exception;

    WrapReturn decodeResult(byte[] inBytes) throws Exception;



}
