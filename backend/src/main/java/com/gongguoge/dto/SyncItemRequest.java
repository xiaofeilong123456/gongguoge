package com.gongguoge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 离线同步的单个操作。
 *
 * <p>注意：type / content 只在 create / update 时必填，因此这里不加 @NotBlank，
 * 由 RecordService 按 op 分支校验，避免「删除操作却因缺 content 被参数校验拦下」。</p>
 */
@Data
public class SyncItemRequest {

    /** 客户端生成的稳定 ID，同一笔记录的所有操作共用 */
    @NotBlank(message = "clientId不能为空")
    private String clientId;

    /** create / update / delete / purge，缺省视为 create */
    private String op;

    @Min(value = 1, message = "类型只能是1或2")
    @Max(value = 2, message = "类型只能是1或2")
    private Integer type;

    private Long categoryId;

    /** 分类名：离线时客户端可能拿不到 categoryId，用名称回退查找 */
    private String categoryName;

    @Size(max = 200, message = "记录内容最长200个字符")
    private String content;

    private LocalDate recordDate;

    private LocalDateTime updatedAt;

    /**
     * 服务端记录 ID。仅用于 delete / purge 兜底：
     * 老数据可能没有 clientId，此时客户端只能靠服务端 ID 定位记录。
     */
    private Long serverId;
}
