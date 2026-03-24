package com.atguigu.tingshu.search.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author atguigu-mqx
 * @ClassName ThreadPoolExecutorConfig
 * @description: TODO
 * @date 2026年02月25日
 * @version: 1.0
 */
@Component
@Slf4j
public class ThreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        //  获取服务器的内核数;
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                corePoolSize * 2,
                corePoolSize * 4,
                3L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                Executors.defaultThreadFactory(),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.error("核心线程数据:\t" + executor.getPoolSize() + "最大线程数" + executor.getMaximumPoolSize() + "任务队列大小" + executor.getQueue());
                        log.error("任务被拒绝了...");
                        //  在执行一次;
                        executor.execute(r);
                    }
                }
        );
    }
}
