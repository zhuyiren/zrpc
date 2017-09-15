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

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by zhuyiren on 2017/6/29.
 */
public class ArgumentHolder implements Serializable {

    private static final long serialVersionUID=1L;

    private final List<ArgumentPair> pairs=new CopyOnWriteArrayList<>();

    private static class ArgumentPair implements Serializable{
        private static final long serialVersionUID=1L;
        final Object argument;
        final Class argumentClass;

        public ArgumentPair(Object argument,Class argumentClass){
            this.argument=argument;
            this.argumentClass=argumentClass;
        }
    }


    public ArgumentHolder(){

    }

    public void addArgument(Object argument,Class argumentClass){
        pairs.add(new ArgumentPair(argument,argumentClass));
    }

    public Object[] arguments(){
        Object[] objects = new Object[pairs.size()];
        for (int index = 0; index < objects.length; index++) {
            objects[index]=pairs.get(index).argument;
        }
        return objects;
    }

    public Class[] argumentClasses(){
        Class[] classes = new Class[pairs.size()];
        for (int index = 0; index < classes.length; index++) {
            classes[index]=pairs.get(index).argumentClass;
        }
        return classes;
    }
}
