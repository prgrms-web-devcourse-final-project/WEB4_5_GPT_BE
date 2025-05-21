package com.WEB4_5_GPT_BE.unihub.domain.course.service;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseRedisCounterService {
    private final RedissonClient redisson;

    public void syncCourseCounters(Course course) {
        String keyCapacity = "course:" + course.getId() + ":capacity";
        String keyEnrolled = "course:" + course.getId() + ":enrolled";
        redisson.getAtomicLong(keyCapacity).set(course.getCapacity());
        redisson.getAtomicLong(keyEnrolled).set(course.getEnrolled());
    }

    public void deleteCourseCounters(Course course) {
        String keyCapacity = "course:" + course.getId() + ":capacity";
        String keyEnrolled = "course:" + course.getId() + ":enrolled";
        redisson.getAtomicLong(keyCapacity).delete();
        redisson.getAtomicLong(keyEnrolled).delete();
    }
}