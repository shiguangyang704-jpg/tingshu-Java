package com.atguigu.tingshu.dispatch.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author atguigu-mqx
 * @ClassName TestTask
 * @description: TODO
 * @date 2026年03月10日
 * @version: 1.0
 */
@Component
public class TestTask {

    /**
     * 测试定时任务
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void testTask(){
        System.out.println("测试定时任务");
        //  远程调用排行榜方法.
    }
}
