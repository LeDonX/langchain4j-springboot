package com.ledon.langchain4j_springboot.config;

//import com.ledon.langchain4j_springboot.service.ToolsService;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.*;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    public interface Assistant {
        String chat(String message);

        // 流式响应
        TokenStream stream(String message);

        /*@SystemMessage("""
                您是“Tuling”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                您正在通过在线聊天系统与客户互动。
                在提供有关预订或取消预订的信息之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
                请讲中文。
                今天的日期是 {{current_date}}.
                """)*/
        TokenStream stream(@UserMessage String message, @V("current_date") String currentDate);
    }

    @Bean
    public EmbeddingStore embeddingStore() {
        return new InMemoryEmbeddingStore();
    }

    @Bean
    public Assistant assistant(OllamaChatModel ollamaChatModel,
                               OllamaStreamingChatModel ollamaStreamingChatModel,
//                               ToolsService toolsService,
                               EmbeddingStore embeddingStore,
                               OllamaEmbeddingModel ollamaEmbeddingModel
    ) {
        // 对话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 内容检索器
//        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
//                .embeddingStore(embeddingStore)
//                .embeddingModel(qwenEmbeddingModel)
//                .maxResults(5) // 最相似的5个结果
//                .minScore(0.6) // 只找相似度在0.6以上的内容
//                .build();

        // 为Assistant动态代理对象  chat  --->  对话内容存储ChatMemory----> 聊天记录ChatMemory取出来 ---->放入到当前对话中
        Assistant assistant = AiServices.builder(Assistant.class)
//                .tools(toolsService)
//                .contentRetriever(contentRetriever)
                .chatModel(ollamaChatModel)
                .streamingChatModel(ollamaStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
        return assistant;
    }


    public interface AssistantUnique {

        String chat(@MemoryId int memoryId, @UserMessage String userMessage);

        // 流式响应
        TokenStream stream(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    @Bean
    public AssistantUnique assistantUnique(OllamaChatModel ollamaChatModel,
                                           OllamaStreamingChatModel ollamaStreamingChatModel) {

        AssistantUnique assistant = AiServices.builder(AssistantUnique.class)
                .chatModel(ollamaChatModel)
                .streamingChatModel(ollamaStreamingChatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder().maxMessages(10)
                                .id(memoryId).build()
                )
                .build();

        return assistant;
    }


    @Bean
    public AssistantUnique assistantUniqueStore(OllamaChatModel ollamaChatModel,
                                                OllamaStreamingChatModel ollamaStreamingChatModel) {

        PersistentChatMemoryStore store = new PersistentChatMemoryStore();

        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();

        AssistantUnique assistant = AiServices.builder(AssistantUnique.class)
                .chatModel(ollamaChatModel)
                .streamingChatModel(ollamaStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
        return assistant;
    }
}