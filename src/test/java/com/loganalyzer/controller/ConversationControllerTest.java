package com.loganalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.ai.ConversationalAgent;
import com.loganalyzer.model.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversationalAgent conversationalAgent;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void chat_returnsAnswerWithSessionId() throws Exception {
        when(conversationalAgent.chat("session-1", "What errors?"))
                .thenReturn("Found 2 database errors.");

        mockMvc.perform(post("/api/logs/conversation/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest("session-1", "What errors?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.answer").value("Found 2 database errors."));
    }

    @Test
    void chat_differentSessions_isolatedMemory() throws Exception {
        when(conversationalAgent.chat("session-A", "Show errors"))
                .thenReturn("Errors from session A context");
        when(conversationalAgent.chat("session-B", "Show errors"))
                .thenReturn("Errors from session B context");

        mockMvc.perform(post("/api/logs/conversation/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest("session-A", "Show errors"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-A"))
                .andExpect(jsonPath("$.answer").value("Errors from session A context"));

        mockMvc.perform(post("/api/logs/conversation/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest("session-B", "Show errors"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-B"))
                .andExpect(jsonPath("$.answer").value("Errors from session B context"));
    }
}
