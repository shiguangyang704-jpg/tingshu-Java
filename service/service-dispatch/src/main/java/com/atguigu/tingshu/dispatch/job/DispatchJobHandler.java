package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.tingshu.model.dispatch.XxlJobLog;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchJobHandler {

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private SearchFeignClient searchFeignClient;

    @XxlJob("updateRangking")
    public void updateRangking() {
        log.info("开始更新排行榜");
        XxlJobLog xxlJobLog = new XxlJobLog();
        long startTime = System.currentTimeMillis();
        try {
            xxlJobLog.setJobId(XxlJobHelper.getJobId());
            //  业务逻辑;
            searchFeignClient.updateLatelyAlbumRanking();
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            log.error("更新排行榜失败！");
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
        }
        int times = (int) (System.currentTimeMillis() - startTime);
        xxlJobLog.setTimes(times);
        xxlJobLogMapper.insert(xxlJobLog);
        log.info("结束更新排行榜");
    }
}