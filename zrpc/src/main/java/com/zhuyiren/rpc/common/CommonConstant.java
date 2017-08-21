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

package com.zhuyiren.rpc.common;

import com.zhuyiren.rpc.handler.ArgumentHolder;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Created by zhuyiren on 2017/8/4.
 */
public final class CommonConstant {


    public static final Schema<ArgumentHolder> ARGUMENT_HELPER_SCHEMA= RuntimeSchema.getSchema(ArgumentHolder.class);

    public static final Schema<WrapReturn> WRAP_RETURN_SCHEMA =RuntimeSchema.getSchema(WrapReturn.class);

    public static final String IDLE_PING="ping";

    public static final String IDLE_PONG="pong";


}