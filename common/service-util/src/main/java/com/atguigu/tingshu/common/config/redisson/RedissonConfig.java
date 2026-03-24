package com.atguigu.tingshu.common.config.redisson;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * redisson配置信息
 */
@Data
@Configuration
@ConfigurationProperties("spring.data.redis")
public class RedissonConfig {

    private String host;

    private String password;

    private String port;

    private int timeout = 3000;
    private static String ADDRESS_PREFIX = "redis://";

    /**
     * 自动装配
     *
     */
    @Bean
    RedissonClient redissonSingle() {
        //  创建一个配置对象;
        Config config = new Config();
        //  判断地址是否为空
        if(StringUtils.isEmpty(host)){
            throw new RuntimeException("host is  empty");
        }
        //  集群版;
//        ClusterServersConfig clusterServersConfig = config.useClusterServers().addNodeAddress("redis://192.168.56.10:7000", "redis://192.168.56.10:7001", "redis://192.168.56.10:7002", "redis://192.168.56.10:7003", "redis://192.168.56.10:7004", "redis://192.168.56.10:7005");

        //  单节点：
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(ADDRESS_PREFIX + this.host + ":" + port)
                .setTimeout(this.timeout);
        if(!StringUtils.isEmpty(this.password)) {
            serverConfig.setPassword(this.password);
        }
        return Redisson.create(config);
    }
}