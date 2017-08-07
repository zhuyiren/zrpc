package com.zhuyiren.rpc.engine;

import com.zhuyiren.rpc.common.WrapReturn;
import com.zhuyiren.rpc.handler.ArgumentHelper;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;

import static com.zhuyiren.rpc.common.SchemaConstant.ARGUMENT_HELPER_SCHEMA;
import static com.zhuyiren.rpc.common.SchemaConstant.WRAP_RETURN_SCHEMA;

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
