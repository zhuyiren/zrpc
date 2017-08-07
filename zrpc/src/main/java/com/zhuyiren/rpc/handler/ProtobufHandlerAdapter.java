package com.zhuyiren.rpc.handler;

import com.google.protobuf.BlockingService;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.zhuyiren.rpc.common.Packet;

/**
 * Created by zhuyiren on 2017/5/21.
 */
public class ProtobufHandlerAdapter implements RequestHandlerAdapter {


    @Override
    public Packet handle(Packet request, Object handler) {
        BlockingService blockingService = (BlockingService) handler;
        try {
            Descriptors.MethodDescriptor method = blockingService.getDescriptorForType().findMethodByName(request.getMethodName());
            Message argument = blockingService.getRequestPrototype(method).newBuilderForType().mergeFrom(request.getEntity()).build();
            Message resultBody = blockingService.callBlockingMethod(method, null, argument);
            Packet response = new Packet(request);
            response.setEntity(resultBody.toByteArray());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean support(Packet request) {
        return "pb".equals(request.getType());
    }


    @Override
    public RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter) {
        throw new UnsupportedOperationException();
    }
}
