package com.insidertracker.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; // List import (혹시 모르니 추가)
import java.util.List; // List import (혹시 모르니 추가)


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

    /**
     * [sec-api.io] Ticker로 Form 4(내부자 거래) 상세 목록을 반환
     */
    public String getInsiderTransactions(String ticker, String period, String filter) throws Exception {

        System.out.println("### 1. sec-api.io 조회 (Ticker: " + ticker + ", Period: " + period + ", Filter: " + filter + ")");

        long monthsToSubtract = Long.parseLong(period.replace("m", ""));
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusMonths(monthsToSubtract);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String toDate = today.format(formatter);

        // 1. 🔽🔽 [수정] 🔽🔽
        // 쿼리문의 필드 이름을 "coding.code"로 수정합니다.
        String baseQuery = "issuer.tradingSymbol:\"" + ticker.toUpperCase() + "\"" +
                " AND periodOfReport:[" + fromDate + " TO " + toDate + "]";

        if ("PS_ONLY".equals(filter)) {
            baseQuery += " AND (nonDerivativeTable.transactions.coding.code:\"P\"" + // "coding.code"로 수정
                    " OR nonDerivativeTable.transactions.coding.code:\"S\")";  // "coding.code"로 수정
        }

        String query = baseQuery;
        System.out.println("### 검색 쿼리: " + query);

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
                .onErrorResume(e -> Mono.just("{\"error\":\"sec-api.io 호출 실패: " + e.getMessage() + "\"}"))
                .block();

        if (jsonResponse == null || jsonResponse.contains("error")) {
            throw new RuntimeException("sec-api.io API 응답 오류: " + jsonResponse);
        }

        System.out.println("### 2. sec-api.io 파싱 완료.");
        return jsonResponse;
    }

    /**
     * [신규] 최신 피드: 미국 시장 "전체"의 "최신 50개" '진짜' 거래(P/S)를 가져옵니다.
     */
    public String getDailyFeed() throws Exception {

        System.out.println("### 1. sec-api.io로 '최신 피드' 조회 시작");

        // 2. 🔽🔽 [수정] 🔽🔽
        // 여기 쿼리문도 "coding.code"로 수정합니다.
        String query = "(nonDerivativeTable.transactions.coding.code:\"P\"" +
                " OR nonDerivativeTable.transactions.coding.code:\"S\")";

        System.out.println("### 최신 피드 쿼리: " + query);

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
                .onErrorResume(e -> Mono.just("{\"error\":\"sec-api.io (최신 피드) 호출 실패: " + e.getMessage() + "\"}"))
                .block();

        if (jsonResponse == null || jsonResponse.contains("error")) {
            throw new RuntimeException("sec-api.io API (최신 피드) 응답 오류: " + jsonResponse);
        }

        System.out.println("### 2. 최신 피드 파싱 완료.");
        return jsonResponse;
    }

    public String getQuote(String ticker) {
        System.out.println("### 3. Finnhub 키 확인: " + finnhubApiKey);
        // 🔼🔼 [수정] 🔼🔼

        System.out.println("### 3. Finnhub로 현재가 조회 시작 (Ticker: " + ticker + ")");

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
    }

    public String searchTicker(String query) {
        System.out.println("### Finnhub로 티커 검색 시작 (Query: " + query + ")");

        String result = finnhubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search") // 1. Finnhub의 /search 엔드포인트
                        .queryParam("q", query) // 2. 검색어
                        .queryParam("token", finnhubApiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("{\"error\":\"Finnhub /search 호출 실패: " + e.getMessage() + "\"}"))
                .block();

        return result;
    }
}