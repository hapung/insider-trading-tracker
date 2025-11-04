package com.insidertracker.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            String secData = apiService.getInsiderTransactions(ticker, period, filter);
            String finnhubQuote = apiService.getQuote(ticker);

            ObjectNode resultNode = objectMapper.createObjectNode();
            resultNode.set("transactionsResponse", objectMapper.readTree(secData));
            resultNode.set("quote", objectMapper.readTree(finnhubQuote));

            return ResponseEntity.ok(resultNode.toString());

        } catch (Exception e) {
            e.printStackTrace();

            // 1. 🔽🔽 [수정] 🔽🔽
            // "안전한" JSON 객체를 ObjectMapper로 만듭니다.
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage()); // e.getMessage()에 따옴표가 있어도 안전
            return new ResponseEntity<>(
                    errorNode.toString(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    @GetMapping(value = "/daily-feed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDailyFeed() {
        try {
            String data = apiService.getDailyFeed();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();

            // 2. 🔽🔽 [수정] 🔽🔽 (여기도 동일하게 수정)
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return new ResponseEntity<>(errorNode.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchTicker(@RequestParam("q") String query) {
        try {
            String data = apiService.searchTicker(query);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();

            // 3. 🔽🔽 [수정] 🔽🔽 (여기도 동일하게 수정)
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", e.getMessage());
            return new ResponseEntity<>(errorNode.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}