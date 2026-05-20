package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文件数据
 * */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "files_data_table", indexes = {
        @Index(name = "idx_files_data_externalId", columnList = "externalId"),
        @Index(name = "idx_files_data_uploadUserId", columnList = "uploadUserId"),
        @Index(name = "idx_files_data_type", columnList = "type"),
        @Index(name = "idx_files_data_status", columnList = "status"),
})
public class FileData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 关联的菜品对象，店面对象
     */
    private Long externalId;

    /**
     * 服务器保存的文件地址
     */
    private String fileUrl;

    /**
     * 文件原始名
     */
    private String fileOriginalName;

    /**
     * 新的文件名
     */
    private String fileNewName;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 上传时间
     */
    private Long uploadTime;

    /**
     * 上传人
     */
    private Long uploadUserId;

    /**
     * 文件类型 【0 与店面关联的文件， 1 与视食品关联的图片】
     */
    private Integer type;

    /**
     * 后缀名
     */
    private String suffixName;

    /**
     * 视频长度
     * */
    private Double duration;

    private Integer height;

    private Integer width;

    /**
     * 像素数
     * */
    private Long pixelsNumber;


    /**
     * 帧率
     * */
    private Double frameRate;


    /**
     * 详细信息 JSON
     * */
    private String info;


    /**
     * 文件存储状态
     * 0  未保存的临时文件，后期删除
     * 1  保存成功并发布的文件
     * */
    private Integer status;


    /**
     * 加载视频所需要的Key
     * */
    @Transient
    private String key;

    private String ip;

    private String ua;

    private String city;

    /**
     * 文件存储位置 默认 0 本地存储
     * */
    private Integer saveLocation = 0;
}
