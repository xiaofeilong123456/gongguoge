package com.gongguoge.service;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.common.util.PhoneMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${aliyun.sms.enabled:true}")
    private boolean enabled;

    @Value("${aliyun.sms.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.sms.sign-name:}")
    private String signName;

    @Value("${aliyun.sms.template-code:}")
    private String templateCode;

    @Value("${aliyun.sms.endpoint:dypnsapi.aliyuncs.com}")
    private String endpoint;

    public void sendCode(String phone, String code) {
        if (!enabled) {
            // 脱敏后再打日志：手机号属个人信息，不能明文进日志
            log.info("[SMS-MOCK] phone={}, code={}", PhoneMask.mask(phone), code);
            return;
        }

        // 只把「调用 SDK」这一步包进 try，业务结果校验放在外面，
        // 否则下面抛出的 BusinessException 会被通用 catch 再包一层，错误信息变成
        // 「短信发送失败：短信发送失败：xxx」。
        SendSmsVerifyCodeResponse response;
        try {
            Client client = createClient();
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setPhoneNumber(phone)
                    // 模板变量格式与已验证可用的写法保持一致：code 为数字、min 为字符串
                    // 模板 ${code} / ${min} 需与这里一一对应
                    .setTemplateParam("{\"code\":" + code + ",\"min\":\"5\"}");
            RuntimeOptions runtime = new RuntimeOptions();
            response = client.sendSmsVerifyCodeWithOptions(request, runtime);
        } catch (TeaException e) {
            // 带上 code：TeaException 的 message 有时只有 "UNKNOWN"，没有 code 根本无法定位
            log.error("[SMS] send failed, phone={}, code={}, message={}, recommend={}",
                    PhoneMask.mask(phone), e.getCode(), e.getMessage(),
                    e.getData() == null ? null : e.getData().get("Recommend"), e);
            throw new BusinessException("短信发送失败：" + e.getCode() + " / " + e.getMessage());
        } catch (Exception e) {
            log.error("[SMS] send failed, phone={}", PhoneMask.mask(phone), e);
            throw new BusinessException("短信发送失败：" + e.getClass().getSimpleName() + " / " + e.getMessage());
        }

        // 阿里云即使 HTTP 200 也可能在 body.code 里返回业务失败（签名/模板不对、欠费、号码非法），
        // 这里必须校验，否则用户收不到短信却以为发送成功。
        String bizCode = response == null || response.getBody() == null ? null : response.getBody().getCode();
        if (!"OK".equals(bizCode)) {
            String bizMsg = response == null || response.getBody() == null
                    ? "短信服务无响应"
                    : response.getBody().getMessage();
            log.error("[SMS] send rejected, phone={}, bodyCode={}, message={}",
                    PhoneMask.mask(phone), bizCode, bizMsg);
            throw new BusinessException("短信发送失败：" + bizMsg);
        }

        log.info("[SMS] send success, phone={}, requestId={}, bodyCode={}",
                PhoneMask.mask(phone),
                response.getBody().getRequestId(),
                bizCode);
    }

    private Client createClient() throws Exception {
        // ⚠️ 只能设显式 AK/SK，**不要**再 setCredential(new credentials.Client())。
        //
        // 默认凭据链会依次尝试：环境变量 → OIDC → **ECS 实例元数据**。
        // 部署在阿里云 ECS 且未绑定 RAM 角色时，元数据请求会失败，
        // 导致短信接口报出难以定位的 "UNKNOWN"（本地电脑上却正常，因为本机
        // 没有元数据服务，会回退到显式 AK —— 这个差异极难排查）。
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = endpoint;
        return new Client(config);
    }

    /**
     * 启动自检：开启了真实短信但关键配置为空时直接拒绝启动。
     * 否则用户会表现为「验证码收不到」，而日志里只有一个含糊的错误。
     */
    @jakarta.annotation.PostConstruct
    void validateSmsConfig() {
        if (!enabled) {
            log.warn("[SMS] 真实短信已关闭（aliyun.sms.enabled=false），验证码将固定为 mock 值，仅限开发环境");
            return;
        }
        StringBuilder missing = new StringBuilder();
        if (!org.springframework.util.StringUtils.hasText(accessKeyId)) {
            missing.append(" access-key-id");
        }
        if (!org.springframework.util.StringUtils.hasText(accessKeySecret)) {
            missing.append(" access-key-secret");
        }
        if (!org.springframework.util.StringUtils.hasText(signName)) {
            missing.append(" sign-name");
        }
        if (!org.springframework.util.StringUtils.hasText(templateCode)) {
            missing.append(" template-code");
        }
        if (missing.length() > 0) {
            throw new IllegalStateException(
                    "短信已开启但以下配置为空，请检查 app.env：" + missing);
        }
        log.info("[SMS] 真实短信已开启，endpoint={}, signName={}", endpoint, signName);
    }
}
