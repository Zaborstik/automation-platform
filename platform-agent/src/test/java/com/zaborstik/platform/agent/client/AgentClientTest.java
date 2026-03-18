package com.zaborstik.platform.agent.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AgentClient using WireMock to stub HTTP responses.
 */
class AgentClientTest {

    private WireMockServer wireMockServer;
    private AgentClient agentClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        agentClient = new AgentClient(wireMockServer.baseUrl(), Duration.ofSeconds(30));
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void executeShouldReturnSuccessWhenAgentReturns200() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo("/execute"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"message\":\"done\",\"error\":null,\"data\":{},\"executionTimeMs\":5}")));

        AgentCommand command = AgentCommand.click("button#submit", "Click submit");
        AgentResponse response = agentClient.execute(command);

        assertTrue(response.isSuccess());
        assertEquals("done", response.getMessage());
        assertNull(response.getError());
    }

    @Test
    void executeShouldReturnFailureWhenAgentReturnsNon200() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo("/execute"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":false,\"message\":null,\"error\":\"Element not found\",\"data\":{},\"executionTimeMs\":0}")));

        AgentCommand command = AgentCommand.click("button#missing", "Click");
        AgentResponse response = agentClient.execute(command);

        assertFalse(response.isSuccess());
        assertEquals("Element not found", response.getError());
    }

    @Test
    void isAvailableShouldReturnTrueWhenHealthEndpointReturns200() {
        wireMockServer.stubFor(get(urlPathEqualTo("/health"))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(agentClient.isAvailable());
    }

    @Test
    void isAvailableShouldReturnFalseWhenHealthEndpointReturns404() {
        wireMockServer.stubFor(get(urlPathEqualTo("/health"))
            .willReturn(aResponse().withStatus(404)));

        assertFalse(agentClient.isAvailable());
    }

    @Test
    void initializeShouldReturnSuccessWhenAgentReturns200() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo("/initialize"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"message\":\"Browser initialized\",\"error\":null,\"data\":{},\"executionTimeMs\":100}")));

        AgentResponse response = agentClient.initialize("https://example.com", true);

        assertTrue(response.isSuccess());
        assertEquals("Browser initialized", response.getMessage());
    }

    @Test
    void initializeShouldReturnFailureWhenAgentReturnsError() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo("/initialize"))
            .willReturn(aResponse().withStatus(500)));

        AgentResponse response = agentClient.initialize("https://example.com", true);

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        assertTrue(response.getError().contains("500"));
    }

    @Test
    void closeShouldReturnSuccessWhenAgentReturns200() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo("/close"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"message\":\"Closed\",\"error\":null,\"data\":{},\"executionTimeMs\":0}")));

        AgentResponse response = agentClient.close();

        assertTrue(response.isSuccess());
    }

    @Test
    void closeShouldReturnFailureWhenAgentReturnsNon200() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo("/close"))
            .willReturn(aResponse().withStatus(500)));

        AgentResponse response = agentClient.close();

        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("Failed to close agent"));
    }

    @Test
    void constructorShouldNormalizeTrailingSlashFromBaseUrl() throws Exception {
        String baseUrlWithSlash = wireMockServer.baseUrl() + "/";
        AgentClient clientWithSlash = new AgentClient(baseUrlWithSlash, Duration.ofSeconds(30));
        wireMockServer.stubFor(post(urlPathEqualTo("/execute"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"message\":\"ok\",\"error\":null,\"data\":{},\"executionTimeMs\":0}")));

        AgentResponse response = clientWithSlash.execute(AgentCommand.explain("test"));

        assertTrue(response.isSuccess(), "Client with trailing slash URL should work correctly");
    }
}
