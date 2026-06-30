package com.smartticket.ai.prompt;

import org.springframework.stereotype.Component;

/** AI Prompt 拼装：强约束模型输出严格 JSON（B5 可解析性）。 */
@Component
public class PromptBuilder {

    public String build(String title, String content) {
        return """
                你是企业 IT/后勤工单分类助手。请阅读报修内容，判断它的「类别」和「优先级」。
                类别只能从以下英文枚举中选一个：
                  NETWORK(网络) HARDWARE(硬件) ACCOUNT(账号) SOFTWARE(软件) LOGISTICS(后勤)
                优先级只能从以下英文枚举中选一个：
                  HIGH(高,影响范围大/紧急) MID(中) LOW(低)
                只输出一个 JSON 对象，不要任何解释、不要 markdown 代码块，格式严格如下：
                {"category":"<枚举>","priority":"<枚举>"}

                工单标题：%s
                工单描述：%s
                """.formatted(safe(title), safe(content));
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").trim();
    }
}
