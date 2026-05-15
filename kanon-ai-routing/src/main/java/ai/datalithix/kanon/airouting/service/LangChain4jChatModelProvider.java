package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ModelProfile;
import dev.langchain4j.model.chat.ChatLanguageModel;

public interface LangChain4jChatModelProvider {
    ChatLanguageModel chatModel(ModelProfile modelProfile);
}
