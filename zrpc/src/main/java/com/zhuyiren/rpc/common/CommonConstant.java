package com.zhuyiren.rpc.common;

import com.zhuyiren.rpc.handler.ArgumentHelper;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Created by zhuyiren on 2017/8/4.
 */
public final class CommonConstant {


    public static final Schema<ArgumentHelper> ARGUMENT_HELPER_SCHEMA= RuntimeSchema.getSchema(ArgumentHelper.class);

    public static final Schema<WrapReturn> WRAP_RETURN_SCHEMA =RuntimeSchema.getSchema(WrapReturn.class);

    public static final String IDLE_PING="ping";

    public static final String IDLE_PONG="pong";


}
