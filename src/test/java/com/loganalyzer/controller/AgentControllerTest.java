package com.loganalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.ai.LogAnalysisAgent;
import com.loganalyzer.model.AgentQueryRequest;
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

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogAnalysisAgent agent;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void query_returnsAgentAnswer() throws Exception {
        when(agent.chat("What errors occurred?")).thenReturn("Found 2 database timeout errors.");

        mockMvc.perform(post("/api/logs/agent/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgentQueryRequest("What errors occurred?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Found 2 database timeout errors."));
    }

    @Test
    void query_noResults_returnsAgentMessage() throws Exception {
        when(agent.chat("anything")).thenReturn("No relevant logs found.");

        mockMvc.perform(post("/api/logs/agent/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgentQueryRequest("anything"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("No relevant logs found."));
    }
}
