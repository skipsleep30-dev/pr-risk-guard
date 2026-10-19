package com.ankit.prguard.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsAnalysis() throws Exception {
        mockMvc.perform(post("/api/analyses")
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repository").value("acme/identity"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.humanReviewRequired").value(true));

        mockMvc.perform(get("/api/analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repository").value("acme/identity"));
    }
}
