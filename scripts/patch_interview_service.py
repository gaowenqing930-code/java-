# -*- coding: utf-8 -*-
import io

p = r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview\service\impl\InterviewServiceImpl.java"
s = io.open(p, encoding="utf-8").read()

# 1) import @Value
s = s.replace(
    "import org.springframework.beans.factory.ObjectProvider;",
    "import org.springframework.beans.factory.ObjectProvider;\nimport org.springframework.beans.factory.annotation.Value;",
)

# 2) 加字段
s = s.replace(
    "    private final ObjectProvider<ChatModel> chatModelProvider;",
    """    private final ObjectProvider<ChatModel> chatModelProvider;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String aiModel;""",
)

# 3) callModel 补 model
old = """        ChatClient client = ChatClient.builder(model)
                .defaultOptions(OpenAiChatOptions.builder().maxTokens(maxTokens))
                .build();"""
new = """        OpenAiChatOptions.Builder optBuilder = OpenAiChatOptions.builder().maxTokens(maxTokens);
        if (StringUtils.hasText(aiModel)) {
            optBuilder.model(aiModel);
        }
        ChatClient client = ChatClient.builder(model)
                .defaultOptions(optBuilder)
                .build();"""
assert old in s, "callModel block not found"
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("patched")
