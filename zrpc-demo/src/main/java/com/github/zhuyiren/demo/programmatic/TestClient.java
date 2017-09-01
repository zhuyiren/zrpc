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

package com.github.zhuyiren.demo.programmatic;

import com.github.zhuyiren.demo.model.TeacherInfo;
import com.github.zhuyiren.demo.service.StudentService;
import com.github.zhuyiren.demo.service.impl.StudentServiceImpl;
import com.zhuyiren.rpc.common.DefaultClient;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import com.zhuyiren.rpc.handler.ProtostuffRequestHandlerAdapter;
import io.protostuff.ProtostuffIOUtil;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public class TestClient {


    public static void main(String[] args)throws Exception {
        DefaultClient client = new DefaultClient("192.168.78.30:2181", "zrpc-demo", 4, false);
        StudentService studentService = client.exportService(ProtostuffEngine.class, StudentService.class, null);
        TeacherInfo teacher = studentService.getTeacher(3);
        StudentService local=new StudentServiceImpl();
        TeacherInfo localTeacher = local.getTeacher(3);
        System.out.println(teacher.equals(localTeacher));
    }
}
