package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;

public interface VodService {

    /**
     * 根据流媒体文件Id获取流媒体对象数据
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    /**
     * 删除流媒体文件
     * @param mediaFileId
     */
    Boolean deleteVod(String mediaFileId);
}
