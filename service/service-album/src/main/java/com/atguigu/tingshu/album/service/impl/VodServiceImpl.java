package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Override
    public Boolean deleteVod(String mediaFileId) {
        try {
            // 密钥信息从环境变量读取，需要提前在环境变量中设置 TENCENTCLOUD_SECRET_ID 和 TENCENTCLOUD_SECRET_KEY
            // 使用环境变量方式可以避免密钥硬编码在代码中，提高安全性
            // 生产环境建议使用更安全的密钥管理方案，如密钥管理系统(KMS)、容器密钥注入等
            // 请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());

            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            DeleteMediaResponse resp = client.DeleteMedia(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            return true;
        } catch (TencentCloudSDKException e) {
            log.error(e.toString(), e);
        }
        //  返回数据
        return false;
    }

    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {
        //  创建对象
        TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
        //  调用api:
        try {
            // 密钥信息从环境变量读取，需要提前在环境变量中设置 TENCENTCLOUD_SECRET_ID 和 TENCENTCLOUD_SECRET_KEY
            // 使用环境变量方式可以避免密钥硬编码在代码中，提高安全性
            // 生产环境建议使用更安全的密钥管理方案，如密钥管理系统(KMS)、容器密钥注入等
            // 请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);

            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);
            //  获取到返回数据，赋值：
            if (null != resp) {
                MediaInfo mediaInfo = resp.getMediaInfoSet()[0];

                //  赋值：
                trackMediaInfoVo.setMediaUrl(mediaInfo.getBasicInfo().getMediaUrl());
                trackMediaInfoVo.setType(mediaInfo.getBasicInfo().getType());
                trackMediaInfoVo.setSize(mediaInfo.getMetaData().getSize());
                trackMediaInfoVo.setDuration(mediaInfo.getMetaData().getDuration());
                //  返回数据;
                return trackMediaInfoVo;
            }
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        //  返回数据
        return trackMediaInfoVo;
    }
}
