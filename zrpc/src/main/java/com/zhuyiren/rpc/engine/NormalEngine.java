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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
    public byte[] encodeArgument(Object[] arguments) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        if (arguments == null) {
            arguments = new Object[]{};
        }
        Class[] classes=new Class[arguments.length];
        for (int index = 0; index < classes.length; index++) {
            classes[index]=arguments[index].getClass();
        }
        oos.writeObject(new ArgumentHelper(arguments,classes));
        return bos.toByteArray();
    }

    @Override
    public ArgumentHelper decodeArgument(byte[] inBytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(inBytes);

        ObjectInputStream ois = new ObjectInputStream(bis);
        return ((ArgumentHelper) ois.readObject());
    }

    @Override
    public byte[] encodeResult(WrapReturn result) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(result);
        return bos.toByteArray();
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(inBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (WrapReturn) ois.readObject();
    }
}
