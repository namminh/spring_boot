#!/usr/bin/env groovy
@Grab('org.springframework.boot:spring-boot-starter-webflux:3.2.5')
@Grab('com.slack.api:slack-api-client:1.38.1')

import org.springframework.web.reactive.function.client.WebClient;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;

String baseUrl = System.getenv('MONITOR_BASE_URL') ?: 'http://localhost:8080';
String endpoint = System.getenv('MONITOR_METRICS_PATH') ?: '/api/v1/payments/metrics/status-count';
String slackToken = System.getenv('SLACK_BOT_TOKEN');
String slackChannel = System.getenv('SLACK_CHANNEL') ?: '#corebank-alerts';
int errorThreshold = (System.getenv('FAILED_THRESHOLD') ?: '3') as int;

WebClient client = WebClient.builder()
    .baseUrl(baseUrl)
    .build();

Map<String, Object> stats = client.get()
    .uri(endpoint)
    .retrieve()
    .bodyToMono(Map)
    .blockOptional()
    .orElseGet({ [:] } as Map);

int failedCount = (stats['FAILED'] ?: 0) as int;
println "[INFO] Metrics snapshot: ${stats}";

if (failedCount > errorThreshold) {
    String message = "⚠️ Payment orchestrator báo ${failedCount} giao dịch FAILED (ngưỡng ${errorThreshold}). Kiểm tra ngay!";
    if (slackToken) {
        try {
            Slack.getInstance().methods(slackToken).chatPostMessage { req ->
                req.channel(slackChannel).text(message);
            };
            println "[INFO] Slack alert sent to ${slackChannel}";
        } catch (IOException | SlackApiException ex) {
            System.err.println "[ERROR] Slack notify failed: ${ex.message}";
        }
    } else {
        println "[WARN] ${message} (chưa cấu hình Slack token)";
    }
} else {
    println "[INFO] Failed count ${failedCount} nằm trong ngưỡng an toàn (${errorThreshold}).";
}
