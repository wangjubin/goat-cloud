package com.goat.cloud.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiMcpServer;
import com.goat.cloud.module.ai.entity.AiMcpTool;
import com.goat.cloud.module.ai.mapper.AiMcpServerMapper;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.module.ai.mapper.AiMcpToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author wangjubin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMcpServerService {

    private final AiMcpServerMapper serverMapper;
    private final AiMcpToolMapper toolMapper;
    private final ObjectMapper objectMapper;

    public AiMcpServer registerServer(AiMcpServer server) {
        if (server.getHealthStatus() == null) server.setHealthStatus("UNKNOWN");
        if (server.getStatus() == null) server.setStatus("ENABLED");
        serverMapper.insert(server);
        checkHealth(server.getServerId());
        return server;
    }

    public List<AiMcpTool> discoverTools(Long serverId) {
        AiMcpServer server = serverMapper.selectById(serverId);
        if (server == null) throw new IllegalArgumentException("Server not found: " + serverId);

        try {
            String url = buildMcpUrl(server, "tools/list");
            JsonNode response = RestClient.create().get().uri(url)
                    .headers(h -> applyAuth(h, server)).retrieve().body(JsonNode.class);

            if (response != null && response.has("tools")) {
                List<AiMcpTool> discovered = new ArrayList<>();
                for (JsonNode toolNode : response.path("tools")) {
                    AiMcpTool tool = new AiMcpTool();
                    tool.setToolCode(toolNode.path("name").asText());
                    tool.setToolName(toolNode.path("name").asText());
                    tool.setServerName(server.getServerCode());
                    tool.setTransportType(server.getTransportType());
                    tool.setEndpoint(server.getEndpoint());
                    tool.setInputSchema(toolNode.path("inputSchema").toString());
                    tool.setOutputSchema(toolNode.has("outputSchema") ? toolNode.path("outputSchema").toString() : null);
                    tool.setStatus(CommonStatus.ENABLED);
                    discovered.add(tool);
                }
                return discovered;
            }
        } catch (Exception e) {
            log.warn("Tool discovery failed for server {}: {}", serverId, e.getMessage());
        }

        return toolMapper.selectList(
                new LambdaQueryWrapper<AiMcpTool>().eq(AiMcpTool::getServerName, server.getServerCode()));
    }

    public AiMcpServer checkHealth(Long serverId) {
        AiMcpServer server = serverMapper.selectById(serverId);
        if (server == null) return null;

        try {
            String url = buildMcpUrl(server, "health");
            RestClient.create().get().uri(url)
                    .headers(h -> applyAuth(h, server)).retrieve().body(String.class);
            server.setHealthStatus("HEALTHY");
        } catch (Exception e) {
            server.setHealthStatus("UNHEALTHY");
            log.warn("Health check failed for MCP server {}: {}", serverId, e.getMessage());
        }
        server.setLastHealthCheck(LocalDateTime.now());
        serverMapper.updateById(server);
        return server;
    }

    public List<AiMcpServer> checkAllHealth() {
        List<AiMcpServer> servers = serverMapper.selectList(
                new LambdaQueryWrapper<AiMcpServer>().eq(AiMcpServer::getStatus, "ENABLED"));
        for (AiMcpServer server : servers) checkHealth(server.getServerId());
        return serverMapper.selectList(new LambdaQueryWrapper<AiMcpServer>().eq(AiMcpServer::getStatus, "ENABLED"));
    }

    public String callTool(Long serverId, String toolName, Map<String, Object> arguments) {
        AiMcpServer server = serverMapper.selectById(serverId);
        if (server == null) throw new IllegalArgumentException("Server not found: " + serverId);

        try {
            String url = buildMcpUrl(server, "tools/" + toolName);
            Map<String, Object> body = Map.of("name", toolName, "arguments", arguments);
            return RestClient.create().post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> applyAuth(h, server))
                    .body(body).retrieve().body(String.class);
        } catch (Exception e) {
            log.error("MCP tool call failed: server={}, tool={}", serverId, toolName, e);
            throw new RuntimeException("MCP tool call failed: " + e.getMessage(), e);
        }
    }

    private String buildMcpUrl(AiMcpServer server, String path) {
        String base = server.getEndpoint() == null ? "" : server.getEndpoint().trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.isBlank()) throw new IllegalArgumentException("Server endpoint not configured");
        return base + "/" + path;
    }

    private void applyAuth(HttpHeaders headers, AiMcpServer server) {
        if (server.getAuthConfigJson() == null) return;
        try {
            Map<String, Object> auth = objectMapper.readValue(server.getAuthConfigJson(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            String type = (String) auth.getOrDefault("type", "");
            if ("bearer".equals(type)) headers.setBearerAuth((String) auth.getOrDefault("token", ""));
            else if ("basic".equals(type)) headers.setBasicAuth((String) auth.getOrDefault("username", ""), (String) auth.getOrDefault("password", ""));
            else if ("apikey".equals(type)) headers.set((String) auth.getOrDefault("header", "X-API-Key"), (String) auth.getOrDefault("key", ""));
        } catch (Exception e) {
            log.warn("Failed to parse auth config for server {}", server.getServerCode());
        }
    }
}