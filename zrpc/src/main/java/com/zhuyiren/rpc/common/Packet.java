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

    private void writeDelimitString(String str, DataOutputStream out) throws IOException {
        int length = str.length();
        out.writeShort(length * 2);
        byte[] bytes = new byte[length * 2];
        for (int index = 0; index < length; index++) {
            char c = str.charAt(index);
            bytes[index * 2] = (byte) ((c >>> 8) & 0xFF);
            bytes[index * 2 + 1] = (byte) ((c >>> 0) & 0xFF);
        }
        out.write(bytes);
        //  out.writeUTF(str);
    }

    private String readDelimitString(DataInputStream din) throws IOException {
        short length = din.readShort();
        byte[] bytes = new byte[length];
        char[] chars = new char[length / 2];
        din.read(bytes);
        for (int index = 0; index < length / 2; index++) {
            chars[index] = (char) (bytes[index * 2] << 8 | bytes[index * 2 + 1]);
        }
        return new String(chars);
        //return din.readUTF();
    }

    @Override
    public void write(OutputStream out) throws IOException {
        /*DataOutputStream dout = new DataOutputStream(out);
        dout.writeLong(id);
        writeDelimitString(serviceName,dout);
        writeDelimitString(type,dout);
        writeDelimitString(methodName,dout);
        if(exception==null){
            writeDelimitString("",dout);
        }else {
            writeDelimitString(exception,dout);
        }
        dout.writeInt(entity.length);
        dout.write(entity);
        dout.close();*/

        ProtostuffIOUtil.writeDelimitedTo(out, this, schema, LinkedBuffer.allocate());
    }


    @Override
    public void readFields(InputStream in) throws IOException {

        /*DataInputStream din=new DataInputStream(in);
        this.id=din.readLong();
        this.serviceName=readDelimitString(din);
        this.type=readDelimitString(din);
        this.methodName=readDelimitString(din);
        String temp = readDelimitString(din);
        if(temp.equals("")){
            exception=null;
        }else {
            exception=temp;
        }
        entity=new byte[din.readInt()];
        din.read(entity);
        din.close();*/
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
