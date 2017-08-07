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

package com.zhuyiren.rpc.handler;

import com.zhuyiren.rpc.common.Packet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuyiren on 2017/5/19.
 */
public class RequestHandlerAdapterComposite implements RequestHandlerAdapter {


    List<RequestHandlerAdapter> handlerAdapters=new ArrayList<>();

    public RequestHandlerAdapterComposite(){

    }

    @Override
    public Packet handle(Packet request, Object handler) {
        for (RequestHandlerAdapter item : handlerAdapters) {
            if(item.support(request)){
                return item.handle(request,handler);
            }
        }
        String exception="no such engine type";
        Packet packet = new Packet(request);
        packet.setException(exception);
        return packet;
    }

    @Override
    public boolean support(Packet request) {
        for (RequestHandlerAdapter handlerAdapter : handlerAdapters) {
            if(handlerAdapter.support(request)){
                return true;
            }
        }
        return false;
    }

    @Override
    public RequestHandlerAdapter addHandlerAdapter(RequestHandlerAdapter adapter) {
        this.handlerAdapters.add(adapter);
        return this;
    }
}
