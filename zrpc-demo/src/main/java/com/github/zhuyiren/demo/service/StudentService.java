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

package com.github.zhuyiren.demo.service;

import com.github.zhuyiren.demo.model.TeacherInfo;
import com.zhuyiren.rpc.engine.JsonEngine;
import com.zhuyiren.rpc.engine.ProtostuffEngine;
import com.zhuyiren.rpc.spring.ZRpcService;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
@ZRpcService(value = "hostUserService",serviceName = "com.github.zhuyiren.demo.service.StudentService",client ="demoClient",engine = ProtostuffEngine.class,ifcCls = StudentService.class)
public interface StudentService {


    TeacherInfo getTeacher(int studentId);

}
