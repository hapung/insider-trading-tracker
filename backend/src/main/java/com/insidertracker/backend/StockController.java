package com.insidertracker.backend;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StockController {

    private final ApiService apiService;

    private final ObjectMapper objectMapper;

    @GetMapping(value = "/insider-trades", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getInsiderTransactions(
            @RequestParam("ticker") String ticker,
            @RequestParam(value = "period", defaultValue = "12m") String period,
            @RequestParam(value = "filter", defaultValue = "PS_ONLY") String filter
    ) {
        try {
            // 3. 🔽🔽 [수정] 🔽🔽
            // API를 2개 호출합니다.

            // 호출 1: [sec-api] 내부자 거래 데이터 가져오기
            String secData = apiService.getInsiderTransactions(ticker, period, filter);

            // 호출 2: [Finnhub] 현재 주가 데이터 가져오기
            String finnhubQuote = apiService.getQuote(ticker);

            // 4. 두 개의 JSON 문자열을 하나의 JSON 객체로 합치기
            ObjectNode resultNode = objectMapper.createObjectNode();

            // 5. sec-api 데이터(문자열)를 'transactionsResponse' 필드에 JSON으로 추가
            resultNode.set("transactionsResponse", objectMapper.readTree(secData));

            // 6. Finnhub 데이터(문자열)를 'quote'라는 필드에 JSON으로 추가
            resultNode.set("quote", objectMapper.readTree(finnhubQuote));

            // 7. 합쳐진 JSON을 문자열로 반환
            return ResponseEntity.ok(resultNode.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    "{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    // ... (기존 /insider-trades 메소드) ...

    /**
     * [신규] 일일 피드 API 엔드포인트
     */
    @GetMapping(value = "/daily-feed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDailyFeed() {
        try {
            // 1. ApiService의 새 메소드를 호출 (파라미터 필요 없음)
            String data = apiService.getDailyFeed();
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    "{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchTicker(@RequestParam("q") String query) {
        try {
            // 1. ApiService의 새 메소드를 호출
            String data = apiService.searchTicker(query);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    "{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}