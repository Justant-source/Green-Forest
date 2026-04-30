package com.vgc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @EnableWebSocketMessageBroker 가 생성하는 messageBrokerTaskScheduler (pool=4) 가
 * TaskSchedulingAutoConfiguration 의 @ConditionalOnMissingBean(TaskScheduler.class)
 * 조건을 막아버려, @Scheduled 태스크가 WebSocket 4-스레드 풀을 공유하게 된다.
 *
 * 같은 초에 스케줄된 두 태스크(AttendanceScheduler + EventScheduler)가 동시에 실행되면
 * drawDailyWinner 의 idempotency 체크가 READ COMMITTED 격리 수준 하에서 통과되어
 * 당첨자가 중복 선정된다.
 *
 * 이 설정으로 "taskScheduler" 이름의 전용 단일 스레드 스케줄러를 등록하면
 * ScheduledAnnotationBeanPostProcessor 가 이 빈을 우선 사용하고,
 * WebSocket 스케줄러는 WebSocket 전용으로만 쓰인다.
 */
@Configuration
public class SchedulingConfig {

    @Bean(name = "taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }
}
