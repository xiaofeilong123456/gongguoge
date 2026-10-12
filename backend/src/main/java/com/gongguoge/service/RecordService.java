package com.gongguoge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.dto.RecordRequest;
import com.gongguoge.dto.SyncItemRequest;
import com.gongguoge.mapper.CategoryMapper;
import com.gongguoge.mapper.MeritRecordMapper;
import com.gongguoge.pojo.Category;
import com.gongguoge.pojo.MeritRecord;
import com.gongguoge.vo.RecordVO;
import com.gongguoge.vo.TodayVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final MeritRecordMapper meritRecordMapper;
    private final CategoryMapper categoryMapper;

    public RecordVO add(RecordRequest request, Long userId) {
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (!category.getType().equals(request.getType())) {
            throw new BusinessException("分类类型与记录类型不一致");
        }
        if (request.getClientId() != null) {
            MeritRecord existing = meritRecordMapper.selectOne(new LambdaQueryWrapper<MeritRecord>()
                    .eq(MeritRecord::getUserId, userId)
                    .eq(MeritRecord::getClientId, request.getClientId())
                    .last("LIMIT 1"));
            if (existing != null) {
                // 幂等：同一 clientId 重复提交（如弱网重试）返回已存在记录，而不是报错
                return toVO(existing);
            }
        }

        MeritRecord record = new MeritRecord();
        record.setUserId(userId);
        record.setType(request.getType());
        record.setCategoryId(request.getCategoryId());
        record.setContent(request.getContent());
        record.setRecordDate(request.getRecordDate() == null ? LocalDate.now() : request.getRecordDate());
        record.setClientId(request.getClientId());
        meritRecordMapper.insert(record);
        return toVO(record);
    }

    public RecordVO update(Long id, RecordRequest request, Long userId) {
        MeritRecord record = meritRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "记录不存在");
        }
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || !category.getType().equals(request.getType())) {
            throw new BusinessException("分类不存在或类型不一致");
        }
        record.setType(request.getType());
        record.setCategoryId(request.getCategoryId());
        record.setContent(request.getContent());
        if (request.getRecordDate() != null) {
            record.setRecordDate(request.getRecordDate());
        }
        meritRecordMapper.updateById(record);
        return toVO(record);
    }

    public void delete(Long id, Long userId) {
        int rows = meritRecordMapper.softDeleteById(id, userId);
        if (rows == 0) {
            throw new BusinessException(404, "记录不存在");
        }
    }

    public TodayVO today(Long userId) {
        LocalDate today = LocalDate.now();
        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .eq(MeritRecord::getRecordDate, today)
                .orderByDesc(MeritRecord::getCreateTime));

        TodayVO vo = new TodayVO();
        vo.setMeritCount(records.stream().filter(r -> r.getType() == 1).count());
        vo.setFaultCount(records.stream().filter(r -> r.getType() == 2).count());
        vo.setList(records.stream().map(this::toVO).toList());
        return vo;
    }

    public List<RecordVO> listByDate(Long userId, LocalDate date) {
        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .eq(MeritRecord::getRecordDate, date)
                .orderByDesc(MeritRecord::getCreateTime));
        return records.stream().map(this::toVO).toList();
    }

    public List<RecordVO> listAll(Long userId) {
        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .orderByDesc(MeritRecord::getRecordDate)
                .orderByDesc(MeritRecord::getCreateTime));
        return records.stream().map(this::toVO).toList();
    }

    public List<RecordVO> recycleList(Long userId) {
        return meritRecordMapper.selectRecycleByUser(userId).stream().map(this::toVO).toList();
    }

    public void restore(Long id, Long userId) {
        int rows = meritRecordMapper.restoreById(id, userId);
        if (rows == 0) {
            throw new BusinessException(404, "回收站记录不存在");
        }
    }

    public void permanentDelete(Long id, Long userId) {
        int rows = meritRecordMapper.physicalDeleteById(id, userId);
        if (rows == 0) {
            throw new BusinessException(404, "回收站记录不存在");
        }
    }

    /**
     * 离线批量同步。
     *
     * <p>按 clientId 幂等：同一笔记录（同一 clientId）重复提交不会产生重复数据，
     * 已应用过的操作会记入 applied 便于客户端清理本地队列。</p>
     */
    @Transactional
    public Map<String, Object> syncBatch(List<SyncItemRequest> items, Long userId) {
        List<Map<String, Object>> applied = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        if (items != null) {
            for (SyncItemRequest item : items) {
                if (item == null || item.getClientId() == null || item.getClientId().isBlank()) {
                    continue;
                }
                String op = item.getOp() == null || item.getOp().isBlank() ? "create" : item.getOp().trim().toLowerCase();
                String reason = null;
                Long serverId = null;

                switch (op) {
                    case "create":
                        reason = applyCreate(item, userId);
                        serverId = findServerId(item.getClientId(), userId);
                        break;
                    case "update":
                        reason = applyUpdate(item, userId);
                        serverId = findServerId(item.getClientId(), userId);
                        break;
                    case "delete":
                        reason = applyDelete(item, userId, false);
                        break;
                    case "purge":
                        reason = applyDelete(item, userId, true);
                        break;
                    default:
                        reason = "未知操作类型: " + op;
                }

                if (reason == null) {
                    Map<String, Object> ok = new HashMap<>();
                    ok.put("clientId", item.getClientId());
                    ok.put("op", op);
                    ok.put("serverId", serverId);
                    applied.add(ok);
                } else {
                    Map<String, Object> bad = new HashMap<>();
                    bad.put("clientId", item.getClientId());
                    bad.put("op", op);
                    bad.put("reason", reason);
                    skipped.add(bad);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("applied", applied);
        result.put("skipped", skipped);
        result.put("appliedCount", applied.size());
        result.put("skippedCount", skipped.size());
        return result;
    }

    /** 返回 null 表示成功，否则返回跳过原因 */
    private String applyCreate(SyncItemRequest item, Long userId) {
        MeritRecord existing = findByClientId(item.getClientId(), userId);
        if (existing != null) {
            return null; // 已存在即为成功（幂等）
        }
        Long categoryId = resolveCategoryId(item, userId);
        if (categoryId == null) {
            return "分类不存在";
        }
        if (item.getType() == null) {
            return "类型不能为空";
        }
        if (item.getContent() == null || item.getContent().isBlank()) {
            return "记录内容不能为空";
        }
        MeritRecord record = new MeritRecord();
        record.setUserId(userId);
        record.setType(item.getType());
        record.setCategoryId(categoryId);
        record.setContent(item.getContent());
        record.setRecordDate(item.getRecordDate() == null ? LocalDate.now() : item.getRecordDate());
        record.setClientId(item.getClientId());
        meritRecordMapper.insert(record);
        return null;
    }

    private String applyUpdate(SyncItemRequest item, Long userId) {
        MeritRecord existing = findByClientId(item.getClientId(), userId);
        if (existing == null) {
            // 本地新增了、但服务端没有：按新增处理，避免数据丢失
            return applyCreate(item, userId);
        }
        Long categoryId = resolveCategoryId(item, userId);
        if (item.getType() != null) {
            existing.setType(item.getType());
        }
        if (categoryId != null) {
            existing.setCategoryId(categoryId);
        }
        if (item.getContent() != null && !item.getContent().isBlank()) {
            existing.setContent(item.getContent());
        }
        if (item.getRecordDate() != null) {
            existing.setRecordDate(item.getRecordDate());
        }
        meritRecordMapper.updateById(existing);
        return null;
    }

    private String applyDelete(SyncItemRequest item, Long userId, boolean permanent) {
        MeritRecord target = locateForDelete(item, userId);
        if (target == null) {
            // 数据库里找不到记录，视为「已经删掉了」，幂等成功。
            // 但前提是客户端提供了至少一个真实定位信息；否则前端伪造的 ID
            // （例如老数据没有 clientId 时兜底的 "s_<id>"）会得到假成功，
            // 前端以为删掉了、服务端其实没删 —— 必须报失败让前端保留重试。
            boolean hasClientId = item.getClientId() != null && !item.getClientId().isBlank();
            boolean hasServerId = item.getServerId() != null;
            if (!hasClientId && !hasServerId) {
                return "缺少可用于定位记录的 clientId 或 serverId";
            }
            if (!hasServerId && looksSynthetic(item.getClientId())) {
                return "clientId 形如前端兜底值，无法定位记录（请补传 serverId）";
            }
            return null;
        }
        if (permanent) {
            meritRecordMapper.physicalDeleteById(target.getId(), userId);
        } else {
            meritRecordMapper.softDeleteById(target.getId(), userId);
        }
        return null;
    }

    /** 识别前端兜底生成的合成 ID（形如 s_123），这种 ID 数据库里不存在 */
    private boolean looksSynthetic(String clientId) {
        return clientId != null && clientId.startsWith("s_");
    }

    /** 定位待删除记录：先按 clientId，再按 serverId（覆盖没有 clientId 的老数据） */
    private MeritRecord locateForDelete(SyncItemRequest item, Long userId) {
        if (item.getClientId() != null && !item.getClientId().isBlank()) {
            MeritRecord byClientId = meritRecordMapper.selectByClientIdIncludingDeleted(userId, item.getClientId());
            if (byClientId != null) {
                return byClientId;
            }
        }
        if (item.getServerId() != null) {
            MeritRecord byId = meritRecordMapper.selectById(item.getServerId());
            if (byId != null && userId.equals(byId.getUserId())) {
                return byId;
            }
        }
        return null;
    }

    /**
     * 按 categoryId 或 categoryName 解析分类。
     * 解析顺序：categoryId -> 本人自定义分类同名 -> 系统预设分类同名 -> 该类型的第一个预设分类。
     *
     * <p>最后一层兜底很关键：离线新增时客户端可能既没有 categoryId 也拿不到分类名，
     * 此时不能直接拒绝（否则记录会永远卡在同步队列里），而应落到预设分类。</p>
     */
    private Long resolveCategoryId(SyncItemRequest item, Long userId) {
        if (item.getCategoryId() != null) {
            Category byId = categoryMapper.selectById(item.getCategoryId());
            if (byId != null) {
                return byId.getId();
            }
        }
        if (item.getType() != null && item.getCategoryName() != null && !item.getCategoryName().isBlank()) {
            Category own = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                    .eq(Category::getUserId, userId)
                    .eq(Category::getType, item.getType())
                    .eq(Category::getName, item.getCategoryName())
                    .last("LIMIT 1"));
            if (own != null) {
                return own.getId();
            }
            Category preset = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                    .eq(Category::getUserId, 0L)
                    .eq(Category::getType, item.getType())
                    .eq(Category::getName, item.getCategoryName())
                    .last("LIMIT 1"));
            if (preset != null) {
                return preset.getId();
            }
        }
        // 兜底：该类型下任取一个系统预设分类（默认「习惯」），保证离线记录不丢
        if (item.getType() != null) {
            Category fallback = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                    .eq(Category::getUserId, 0L)
                    .eq(Category::getType, item.getType())
                    .eq(Category::getName, "习惯")
                    .last("LIMIT 1"));
            if (fallback != null) {
                return fallback.getId();
            }
            Category any = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                    .eq(Category::getUserId, 0L)
                    .eq(Category::getType, item.getType())
                    .orderByAsc(Category::getSort)
                    .last("LIMIT 1"));
            if (any != null) {
                return any.getId();
            }
        }
        return null;
    }

    private MeritRecord findByClientId(String clientId, Long userId) {
        return meritRecordMapper.selectOne(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .eq(MeritRecord::getClientId, clientId)
                .last("LIMIT 1"));
    }

    private Long findServerId(String clientId, Long userId) {
        MeritRecord record = findByClientId(clientId, userId);
        return record == null ? null : record.getId();
    }

    private RecordVO toVO(MeritRecord record) {
        RecordVO vo = new RecordVO();
        vo.setId(record.getId());
        vo.setType(record.getType());
        vo.setCategoryId(record.getCategoryId());
        vo.setContent(record.getContent());
        vo.setRecordDate(record.getRecordDate());
        vo.setClientId(record.getClientId());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        if (record.getCategoryId() != null) {
            Category category = categoryMapper.selectById(record.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
        }
        return vo;
    }
}
