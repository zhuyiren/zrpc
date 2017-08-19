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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;

/**
 * Created by zhuyiren on 2017/6/3.
 */
public class NormalEngine extends AbstractEngine implements Engine {

    public static final String ENGINE_NORMAL = "normal";

    @Override
    public String getType() {
        return ENGINE_NORMAL;
    }


    @Override
    public byte[] encodeArgument(ArgumentHolder argumentHolder) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(argumentHolder);
        byte[] result = bos.toByteArray();
        oos.close();
        return result;
    }

    @Override
    public ArgumentHolder decodeArgument(byte[] inBytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(inBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        ArgumentHolder result = (ArgumentHolder) ois.readObject();
        ois.close();
        return result;
    }

    @Override
    public byte[] encodeResult(WrapReturn wrapReturn) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(wrapReturn);
        byte[] result = bos.toByteArray();
        oos.close();
        return result;
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes, Type type) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(inBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        WrapReturn result = (WrapReturn) ois.readObject();
        ois.close();
        return result;
    }
}
