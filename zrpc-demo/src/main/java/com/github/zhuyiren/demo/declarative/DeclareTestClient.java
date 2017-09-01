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

package com.github.zhuyiren.demo.declarative;

import com.github.zhuyiren.demo.model.TeacherInfo;
import com.github.zhuyiren.demo.service.StudentService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public class DeclareTestClient {

    public static void main(String[] args){
        ClassPathXmlApplicationContext context=new ClassPathXmlApplicationContext("client.xml");
        StudentService studentService = context.getBean(StudentService.class);
        TeacherInfo teacher = studentService.getTeacher(3);
        System.out.println(teacher);
    }
}
