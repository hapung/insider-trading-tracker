package com.insidertracker.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ApiService {

    @Qualifier("secApiWebClient")
    private final WebClient secApiWebClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${secapi.api.key}")
    private String secApiKey;

    @Qualifier("finnhubWebClient")
    private final WebClient finnhubWebClient;

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    // ✅ 캐시 + rate limit 관리용 구조
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastCallTime = new ConcurrentHashMap<>();

    private record CacheEntry(String data, Instant savedAt) {}

    /**
     * [sec-api.io] 내부자 거래 조회 (rate limit + 캐시 적용)
     */
    public String getInsiderTransactions(String ticker, String period, String filter) throws Exception {
        System.out.println("### [SEC-API] 내부자 거래 조회 시작: " + ticker);

        // --- rate limit (30초 내 중복 호출 차단)
        Instant now = Instant.now();
        Instant last = lastCallTime.getOrDefault(ticker, Instant.EPOCH);
        if (now.isBefore(last.plusSeconds(30))) {
            System.out.println("### 호출 제한: " + ticker + " 최근 호출로부터 30초 미만");
            return cachedOrError("Rate limit: too frequent requests for " + ticker);
        }
        lastCallTime.put(ticker, now);

        // --- 캐시 조회 (1시간 유효)
        CacheEntry cacheEntry = cache.get(ticker);
        if (cacheEntry != null && now.isBefore(cacheEntry.savedAt().plusSeconds(3600))) {
            System.out.println("### 캐시 사용: " + ticker);
            return cacheEntry.data();
        }

        // --- 쿼리 구성
        long monthsToSubtract = Long.parseLong(period.replace("m", ""));
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusMonths(monthsToSubtract);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String toDate = today.format(formatter);

        String query = "issuer.tradingSymbol:\"" + ticker.toUpperCase() + "\"" +
                " AND periodOfReport:[" + fromDate + " TO " + toDate + "]";
        if ("PS_ONLY".equals(filter)) {
            query += " AND (nonDerivativeTable.transactions.coding.code:\"P\"" +
                    " OR nonDerivativeTable.transactions.coding.code:\"S\")";
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("query", query);
        payload.put("from", "0");
        payload.put("size", "50");
        payload.set("sort", objectMapper.createArrayNode().add(
                objectMapper.createObjectNode().set("filedAt",
                        objectMapper.createObjectNode().put("order", "desc")
                )
        ));

        System.out.println("### SEC-API 요청 쿼리: " + query);

        try {
            String jsonResponse = secApiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/insider-trading")
                            .queryParam("token", secApiKey)
                            .build())
                    .bodyValue(payload.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .onErrorResume(e -> Mono.just("{\"error\":\"sec-api.io 호출 실패: " + e.getMessage() + "\"}"))
                    .block();

            if (jsonResponse == null || jsonResponse.contains("Too Many Requests")) {
                System.out.println("### 429 Too Many Requests 감지됨");
                return cachedOrError("sec-api.io rate limit exceeded (429)");
            }

            if (jsonResponse.contains("\"error\"")) {
                throw new RuntimeException("sec-api.io 응답 오류: " + jsonResponse);
            }

            // 성공 시 캐시에 저장
            cache.put(ticker, new CacheEntry(jsonResponse, now));

            System.out.println("### SEC-API 응답 성공, 캐시 저장 완료");
            return jsonResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return cachedOrError("sec-api.io 예외 발생: " + e.getMessage());
        }
    }

    private String cachedOrError(String message) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("error", message);

        // 캐시된 데이터가 있으면 fallback
        if (!cache.isEmpty()) {
            String anyCache = cache.values().iterator().next().data();
            System.out.println("### 캐시 fallback 반환");
            return anyCache;
        }
        return errorNode.toString();
    }

    /**
     * [최신 피드]
     */
    public String getDailyFeed() {
        try {
            System.out.println("### [SEC-API] 최신 피드 요청");

            String query = "(nonDerivativeTable.transactions.coding.code:\"P\"" +
                    " OR nonDerivativeTable.transactions.coding.code:\"S\")";

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("query", query);
            payload.put("from", "0");
            payload.put("size", "50");
            payload.set("sort", objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode().set("filedAt",
                            objectMapper.createObjectNode().put("order", "desc")
                    )
            ));

            String jsonResponse = secApiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/insider-trading")
                            .queryParam("token", secApiKey)
                            .build())
                    .bodyValue(payload.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .onErrorResume(e -> Mono.just("{\"error\":\"sec-api.io (feed) 호출 실패: " + e.getMessage() + "\"}"))
                    .block();

            if (jsonResponse == null || jsonResponse.contains("\"error\"")) {
                throw new RuntimeException("sec-api.io (feed) 응답 오류: " + jsonResponse);
            }

            return jsonResponse;
        } catch (Exception e) {
            e.printStackTrace();
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return errorNode.toString();
        }
    }

    public String getQuote(String ticker) {
        try {
            String result = finnhubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", ticker)
                            .queryParam("token", finnhubApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> Mono.just("{\"error\":\"Finnhub /quote 호출 실패: " + e.getMessage() + "\"}"))
                    .block();
            return result;
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return errorNode.toString();
        }
    }

    public String searchTicker(String query) {
        try {
            String result = finnhubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("token", finnhubApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> Mono.just("{\"error\":\"Finnhub /search 호출 실패: " + e.getMessage() + "\"}"))
                    .block();
            return result;
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return errorNode.toString();
        }
    }
}
