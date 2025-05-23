package com.WEB4_5_GPT_BE.unihub.global.init.redis;

import com.WEB4_5_GPT_BE.unihub.domain.course.entity.Course;
import com.WEB4_5_GPT_BE.unihub.domain.course.repository.CourseRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"stg", "prod"})
@DependsOn("initProdOrStgData") // 데이터 초기화 이후 동작하도록 설정
public class InitProdOrStgRedisData {

    private final CourseRepository courseRepository;
    private final RedissonClient redisson;

    /**
     * 애플리케이션이 시작될 때 DB에 저장된 모든 강좌를 조회해
     * Redis의 capacity/enrolled 카운터를 동기화합니다.
     */
    @PostConstruct
    public void initCourseCounters() {
        List<Course> courses = courseRepository.findAll();
        log.info("초기화: Redis에 {}개의 강좌 카운터를 동기화합니다.", courses.size());

        // 1) 배치 생성 (파이프라이닝)
        // 여러 Redis 명령을 “파이프라인”처럼 묶어서 한 번에 전송할 수 있는 RBatch API
        RBatch batch = redisson.createBatch();

        for (Course course : courses) {
            String capacityKey = "course:" + course.getId() + ":capacity";
            String enrolledKey = "course:" + course.getId() + ":enrolled";

            // setAsync() 로 예약만 해 두고, batch.execute()로 한 번에 Redis에 전송합니다.
            // 네트워크 왕복(RTT) 횟수가 크게 줄어들기 때문에 대량 초기화 시 성능 이점
            batch.getAtomicLong(capacityKey).setAsync(course.getCapacity());
            batch.getAtomicLong(enrolledKey).setAsync(course.getEnrolled());
        }

        // 3) 한 번의 네트워크 왕복으로 실행
        BatchResult<?> result = batch.execute();

        log.info("초기화 완료: Redis 강좌 카운터 동기화가 끝났습니다. ({} commands)",
                result.getResponses().size());
    }
}

