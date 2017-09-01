package com.github.zhuyiren.demo.service;

import com.github.zhuyiren.demo.model.StudentInfo;

import java.util.List;

/**
 * @author zhuyiren
 * @date 2017/9/2
 */
public interface TeacherService {


    List<StudentInfo> getStudents(long teacherId);

}
