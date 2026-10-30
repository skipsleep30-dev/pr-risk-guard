package com.ankit.prguard.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "demo")
class AnalysisApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void createsAndListsAnalysis() throws Exception {
        MvcResult submitted = mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repository": "acme/identity",
                                  "number": 42,
                                  "title": "Update token validation",
                                  "description": "Security-sensitive change",
                                  "author": "demo-user",
                                  "additions": 220,
                                  "deletions": 40,
                                  "changedFiles": 5,
                                  "files": [
                                    "src/main/java/auth/TokenValidator.java",
                                    "src/test/java/auth/TokenValidatorTest.java"
                                  ],
                                  "testsPresent": true
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.repository").value("acme/identity"))
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        String id = mapper.readTree(submitted.getResponse().getContentAsString()).path("id").asText();
        String completed = "";
        for (int attempt = 0; attempt < 40; attempt++) {
            completed = mockMvc.perform(get("/api/analyses/" + id))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            if (completed.contains("\"status\":\"COMPLETED\"")) break;
            Thread.sleep(50);
        }
        assertThat(completed).contains("\"status\":\"COMPLETED\"");
        assertThat(completed).contains("\"riskLevel\":\"HIGH\"");

        mockMvc.perform(get("/api/analyses/" + id)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("reviewer")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repository").value("acme/identity"));
    }
}
