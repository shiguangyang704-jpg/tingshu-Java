package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ServiceAlbumApplication implements CommandLineRunner {

    @Autowired
    private RedissonClient redissonClient;
    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }
    //  http://localhost:8501/api/search/album/{346578fkgjlt78ot7er5duj}
    @Override
    public void run(String... args) throws Exception {
        //  初始化数据规模与误判率!
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        //  调用方法;
        bloomFilter.tryInit(100000,0.001);
    }
}
