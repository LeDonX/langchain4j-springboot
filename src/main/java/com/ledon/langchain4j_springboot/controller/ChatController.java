package com.ledon.langchain4j_springboot.controller;

import com.ledon.langchain4j_springboot.config.AiConfig;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.TokenStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * @author LeDon
 * @date 2026-04-18 23:19
 */

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    OllamaChatModel ollamaChatModel;

    @Autowired
    OllamaStreamingChatModel ollamaStreamingChatModel;

    @RequestMapping("/chat")
    public String test(@RequestParam(defaultValue = "你是谁") String message) {
        String chat = ollamaChatModel.chat(message);
        return chat;
    }

    @RequestMapping("/stream")
    public Flux<String> stream(@RequestParam(defaultValue = "你是谁") String message) {
        Flux<String> objectFlux = Flux.create(emitter -> {
            ollamaStreamingChatModel.chat(message, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    emitter.next(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    emitter.error(error);
                }
            });
        });
        return objectFlux;
    }

    @Autowired
    AiConfig.Assistant assistant;

    @RequestMapping(value = "/memory_chat")
    public String memoryChat(@RequestParam(defaultValue = "我叫LeDon") String message) {
        return assistant.chat(message);
    }

    @RequestMapping(value = "/memory_stream", produces = "text/stream;charset=UTF-8")
    public Flux<String> memoryStreamChat(@RequestParam(defaultValue = "我是谁") String message, HttpServletResponse response) {
        TokenStream stream = assistant.stream(message, LocalDate.now().toString());

        return Flux.create(sink -> {
            stream.onPartialResponse(s -> sink.next(s))
                    .onCompleteResponse(c -> sink.complete())
                    .onError(sink::error)
                    .start();

        });
    }

    @Autowired
    AiConfig.AssistantUnique assistantUnique;

    @RequestMapping(value = "/memoryId_chat")
    public String memoryChat(@RequestParam(defaultValue = "我是谁") String message, Integer userId) {
        return assistantUnique.chat(userId, message);
    }
}
