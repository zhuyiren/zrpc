package com.zhuyiren.rpc.engine;

import com.zhuyiren.rpc.common.Packet;
import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

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
        Object[] writeObject = new Object[arguments.length * 2];
        for (int index = 0; index < arguments.length; index++) {
            writeObject[index] = arguments[index].getClass();
        }
        System.arraycopy(arguments, 0, writeObject, arguments.length, arguments.length);
        oos.writeObject(writeObject);
        return bos.toByteArray();
    }

    @Override
    public ArgumentHelper decodeArgument(byte[] inBytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(inBytes);

        ObjectInputStream ois = new ObjectInputStream(bis);
        Object[] argumentAndClass = (Object[]) ois.readObject();
        ArgumentHelper result = new ArgumentHelper();
        if (argumentAndClass == null) {
            result.argumentClasses = new Class[]{};
            result.arguments = new Object[]{};
            return result;

        }
        result.arguments = new Object[argumentAndClass.length / 2];
        result.argumentClasses = new Class[argumentAndClass.length / 2];
        System.arraycopy(argumentAndClass, 0, result.argumentClasses, 0, result.arguments.length);
        System.arraycopy(argumentAndClass, result.argumentClasses.length, result.arguments, 0, result.arguments.length);
        return result;
    }

    @Override
    public byte[] encodeResult(WrapReturn result) throws Exception {
        return new byte[0];
    }

    @Override
    public WrapReturn decodeResult(byte[] inBytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(inBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object temp = ois.readObject();
        WrapReturn result = new WrapReturn(temp);
        return result;
    }
}
