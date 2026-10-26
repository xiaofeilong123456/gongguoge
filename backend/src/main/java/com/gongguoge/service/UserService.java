package com.gongguoge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.dto.ProfileUpdateRequest;
import com.gongguoge.mapper.SmsLogMapper;
import com.gongguoge.mapper.UserMapper;
import com.gongguoge.pojo.SmsLog;
import com.gongguoge.pojo.User;
import com.gongguoge.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final DataService dataService;
    private final SmsLogMapper smsLogMapper;

    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return toVO(user);
    }

    public UserVO updateProfile(ProfileUpdateRequest request, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    /**
     * 注销账号：删除账号及其全部个人数据，不可恢复。
     * <p>
     * 顺序很关键——先清业务数据再删用户行，整体包在一个事务里：
     * 任何一步失败都会回滚，不会出现「账号没了但记录还在」的中间态。
     * <p>
     * 删掉用户行之后，JwtInterceptor 每次请求都会查库校验用户是否存在，
     * 因此旧 token 在下一次请求时即返回 401 失效，无需额外的 token 黑名单。
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        // 1) 业务数据：记录（含回收站）、反馈、自定义标签
        dataService.clearAll(userId);
        // 2) 短信发送日志中留有手机号，注销时应一并清除
        if (StringUtils.hasText(user.getPhone())) {
            smsLogMapper.delete(new LambdaQueryWrapper<SmsLog>()
                    .eq(SmsLog::getPhone, user.getPhone()));
        }
        // 3) 账号本身
        userMapper.deleteById(userId);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
