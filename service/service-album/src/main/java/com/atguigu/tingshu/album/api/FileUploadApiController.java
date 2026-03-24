package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    //  @Value("${minio.bucketName}") @RefreshScope
    private String bucketName;

    /**
     * 文件上传
     * @return
     */
    @Operation(summary = "文件上传")
    @PostMapping("/fileUpload")
    public Result fileUpload(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        //  声明一个url 地址;
        String url = "";

        //  minioConstantProperties.getEndpointUrl()
        //  获取上传的文件名称
        //        String originalFilename = file.getOriginalFilename();
        //  file 上传到自己的服务器上，并返回一个url 路径; 使用minio!
        // Create a minioClient with the MinIO server playground, its access key and secret key.
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(minioConstantProperties.getEndpointUrl())
                        .credentials(minioConstantProperties.getAccessKey(), minioConstantProperties.getSecreKey())
                        .build();

        // Make 'asiatrip' bucket if not exist.
        boolean found =
                minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
        if (!found) {
            // Make a new bucket called 'asiatrip'.
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
        } else {
            System.out.println("Bucket "+minioConstantProperties.getBucketName()+" already exists.");
        }
        //  文件名称：  file.getOriginalFilename()=at.guigu.jpg String
        String str = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = UUID.randomUUID() + str;
        //  文件上传：
        minioClient.putObject(
                PutObjectArgs.builder().bucket(minioConstantProperties.getBucketName()).object(fileName).stream(
                                file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
        //  返回数据; http: //192.168.200.130:9000
        url=minioConstantProperties.getEndpointUrl()+"/"+minioConstantProperties.getBucketName()+"/"+fileName;
        System.out.println("url:\t"+ url);
        //  返回数据;
        return Result.ok(url);
    }


}
