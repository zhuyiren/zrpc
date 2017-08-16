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

import com.zhuyiren.rpc.engine.Writable;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.*;
import java.util.Arrays;

/**
 * Created by zhuyiren on 2017/5/18.
 */
public class Packet implements Writable {


    long id;
    String serviceName;
    String type;
    String methodName;
    String exception;
    byte[] entity;


    private static final Schema<Packet> schema = RuntimeSchema.getSchema(Packet.class);

    public Packet() {

    }

    public Packet(String serviceName, String type, String methodName, byte[] entity) {
        this.serviceName = serviceName;
        this.type = type;
        this.methodName = methodName;
        this.entity = entity;
    }

    public Packet(Packet request) {
        this.id = request.id;
        this.serviceName = request.serviceName;
        this.type = request.type;
        this.methodName = request.methodName;
        this.exception = request.exception;
    }




    @Override
    public void write(OutputStream out) throws IOException {
        ProtostuffIOUtil.writeDelimitedTo(out, this, schema, LinkedBuffer.allocate());
    }


    @Override
    public void readFields(InputStream in) throws IOException {
        ProtostuffIOUtil.mergeDelimitedFrom(in, this, schema);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public byte[] getEntity() {
        return entity;
    }

    public void setEntity(byte[] entity) {
        this.entity = entity;
    }


    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "id=" + id +
                ", serviceName='" + serviceName + '\'' +
                ", type='" + type + '\'' +
                ", methodName='" + methodName + '\'' +
                ", entity=" + Arrays.toString(entity) +
                '}';
    }
}
