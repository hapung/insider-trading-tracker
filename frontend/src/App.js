import React, { useState, useEffect } from 'react';
import './App.css';

// 백엔드(Render) 주소
const API_BASE_URL = "https://insider-trading-tracker.onrender.com";

function App() {
    // --- State 선언부 ---
    const [ticker, setTicker] = useState("AAPL");
    const [period, setPeriod] = useState("12m");
    const [filter, setFilter] = useState("PS_ONLY");
    const [transactions, setTransactions] = useState(null);
    const [quote, setQuote] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [dailyFeed, setDailyFeed] = useState(null);
    const [feedError, setFeedError] = useState(null);
    const [searchTerm, setSearchTerm] = useState("AAPL");
    const [suggestions, setSuggestions] = useState([]);

    // --- 함수 선언부 ---

    // '자동완성' API 호출 (useEffect)
    useEffect(() => {
        if (searchTerm.trim() === "") {
            setSuggestions([]);
            return;
        }
        const delayDebounceFn = setTimeout(() => {
            console.log("Fetching suggestions for: ", searchTerm);
            fetch(`${API_BASE_URL}/api/v1/search?q=${searchTerm}`)
                .then(response => response.json())
                .then(jsonData => {
                    if (jsonData.result) {
                        setSuggestions(jsonData.result);
                    } else {
                        setSuggestions([]);
                    }
                })
                .catch(err => {
                    console.error("Suggestion fetch error:", err.message);
                    setSuggestions([]);
                });
        }, 500);
        return () => clearTimeout(delayDebounceFn);
    }, [searchTerm]);


    // '최신 피드' API 호출 함수 (수동)
    const fetchDailyFeed = () => {
        console.log("Fetching daily feed...");
        setFeedError(null);
        setDailyFeed(null); // 피드를 비워서 로딩 상태를 표시

        fetch(`${API_BASE_URL}/api/v1/daily-feed`)
            .then(res => res.json())
            .then(data => {
                if (data.error) throw new Error(data.error);
                setDailyFeed(data.transactions);
            })
            .catch(err => setFeedError(err.message));
    };

    // 🔽🔽 [수정] 🔽🔽
    // 페이지 로드 시 '자동'으로 피드를 부르던 useEffect를 "삭제"했습니다.
    // useEffect(() => {
    //   fetchDailyFeed();
    // }, []);


    // '검색' 버튼 함수
    const fetchData = () => {
        setLoading(true);
        setError(null);
        setTransactions(null);
        setQuote(null);
        const url = `${API_BASE_URL}/api/v1/insider-trades?ticker=${ticker}&period=${period}&filter=${filter}`;
        setSuggestions([]); // 검색 누르면 추천 목록 숨기기
        fetch(url)
            .then(response => response.json())
            .then(jsonData => {
                if (jsonData.error) throw new Error(jsonData.error);
                setTransactions(jsonData.transactionsResponse.transactions);
                setQuote(jsonData.quote);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    };

    // (핸들러 함수들 - 동일)
    const handleSuggestionClick = (symbol) => {
        setTicker(symbol);
        setSearchTerm(symbol);
        setSuggestions([]);
    };
    const handleSearchChange = (e) => {
        const value = e.target.value.toUpperCase();
        setSearchTerm(value);
        setTicker(value);
    };

    // --- 렌더링 ---
    return (
        <div className="App">
            <header className="App-header">
                <h1>내부자 거래 추적기</h1>
            </header>
            <div className="container">
                <div className="main-content">

                    {/* 검색 영역 */}
                    <div className="search-bar">
                        <div className="search-input-wrapper">
                            <input
                                type="text"
                                value={ticker}
                                onChange={handleSearchChange}
                                placeholder="티커 입력 (예: TSLA)"
                            />
                            {suggestions.length > 0 && (
                                <ul className="suggestions-list">
                                    {suggestions.map((suggestion) => (
                                        <li
                                            key={suggestion.symbol}
                                            onClick={() => handleSuggestionClick(suggestion.symbol)}
                                        >
                                            {suggestion.symbol} - {suggestion.description}
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                        <select value={period} onChange={(e) => setPeriod(e.target.value)}>
                            <option value="3m">최근 3개월</option>
                            <option value="6m">최근 6개월</option>
                            <option value="12m">최근 12개월</option>
                        </select>
                        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
                            <option value="PS_ONLY">"진짜" 거래만 (P/S)</option>
                            <option value="ALL">모든 거래 (M, G, F 포함)</option>
                        </select>
                        <button onClick={fetchData} disabled={loading}>
                            {loading ? '검색 중...' : '검색'}
                        </button>
                    </div>

                    {error && <p style={{ color: 'red' }}>검색 오류: {error}</p>}

                    {quote && (
                        <div className="quote-box">
                            {quote.error ? ( <p style={{ color: 'red' }}>주가 지표 로드 실패: {quote.error}</p> ) : (
                                <>
                                    <div className="quote-item">
                                        <h3>현재가 (Current Price)</h3>
                                        <p className={getChangeClassName(quote.d)}>${(quote.c || 0).toFixed(2)}</p>
                                    </div>
                                    <div className="quote-item">
                                        <h3>당일 변동 (Change)</h3>
                                        <p className={getChangeClassName(quote.d)}>
                                            {(quote.d || 0).toFixed(2)} ({(quote.dp || 0).toFixed(2)}%)
                                        </p>
                                    </div>
                                    <div className="quote-item">
                                        <h3>당일 고가 (High)</h3>
                                        <p>${(quote.h || 0).toFixed(2)}</p>
                                    </div>
                                    <div className="quote-item">
                                        <h3>당일 저가 (Low)</h3>
                                        <p>${(quote.l || 0).toFixed(2)}</p>
                                    </div>
                                </>
                            )}
                        </div>
                    )}

                    {transactions && (
                        <RenderMainTable transactions={transactions} filterType={filter} />
                    )}
                </div>

                {/* --- 사이드바 (수동 버튼 추가) --- */}
                <div className="sidebar">
                    <div className="feed-header">
                        <h2>최신 P/S 거래</h2>
                        <button onClick={fetchDailyFeed} className="feed-refresh-btn">
                            ↻
                        </button>
                    </div>
                    <RenderFeed feed={dailyFeed} error={feedError} />
                </div>
            </div>
        </div>
    );
}

// --- 헬퍼 함수들 (이하 동일) ---
function getTransactionType(code) {
    switch (code) {
        case 'P': return 'Buy (매수)';
        case 'S': return 'Sell (매도)';
        case 'M': return 'Option (옵션 행사)';
        case 'G': return 'Gift (증여)';
        case 'F': return 'Tax (세금 납부)';
        default: return code;
    }
}
function getChangeClassName(change) {
    if (change === undefined || change === null) return '';
    return change > 0 ? 'positive' : 'negative';
}
function processFeedData(feed) {
    const psTrades = [];
    if (!feed) return psTrades;
    feed.forEach(item => {
        if (item.nonDerivativeTable && item.nonDerivativeTable.transactions) {
            item.nonDerivativeTable.transactions.forEach(trade => {
                const code = trade.coding.code;
                if (code === 'P' || code === 'S') {
                    psTrades.push({
                        id: item.id + '-' + trade.transactionDate,
                        ticker: item.issuer.tradingSymbol,
                        type: getTransactionType(code),
                        shares: trade.amounts?.shares || 0,
                        price: trade.amounts?.pricePerShare || 0,
                    });
                }
            });
        }
    });
    return psTrades;
}
function processMainData(transactions, filterType) {
    const allTrades = [];
    if (!transactions) return allTrades;
    transactions.forEach(item => {
        if (item.nonDerivativeTable && item.nonDerivativeTable.transactions) {
            item.nonDerivativeTable.transactions.forEach((trade, index) => {
                const code = trade.coding.code;
                if (filterType === "PS_ONLY" && (code !== 'P' && code !== 'S')) {
                    // 스킵
                } else {
                    allTrades.push({
                        id: item.id + '-' + index,
                        reporterName: item.reportingOwner.name,
                        transactionDate: trade.transactionDate,
                        type: getTransactionType(code),
                        shares: trade.amounts?.shares || 0,
                        price: trade.amounts?.pricePerShare || 0,
                    });
                }
            });
        }
    });
    return allTrades;
}
function RenderMainTable({ transactions, filterType }) {
    const processedTransactions = processMainData(transactions, filterType);
    if (processedTransactions.length === 0) {
        return <p>선택한 조건의 거래 내역이 없습니다.</p>;
    }
    return (
        <table className="results-table">
            <thead>
            <tr>
                <th>거래자 (Reporter)</th>
                <th>거래일 (Date)</th>
                <th>유형 (Type)</th>
                <th>수량 (Shares)</th>
                <th>단가 (Price)</th>
            </tr>
            </thead>
            <tbody>
            {processedTransactions.map((trade) => (
                <tr key={trade.id}>
                    <td>{trade.reporterName}</td>
                    <td>{trade.transactionDate}</td>
                    <td className={
                        trade.type.includes('Buy') ? 'positive' :
                            trade.type.includes('Sell') ? 'negative' : ''
                    }>
                        {trade.type}
                    </td>
                    <td>{trade.shares.toLocaleString()} 주</td>
                    <td>${trade.price.toFixed(2)}</td>
                </tr>
            ))}
            </tbody>
        </table>
    );
}
function RenderFeed({ feed, error }) {
    const processedFeed = processFeedData(feed);
    if (error) {
        return <p style={{ color: 'red' }}>피드 오류: {error}</p>;
    }
    if (!feed) {
        return <p>피드 '새로고침(↻)' 버튼을 눌러주세요.</p>;
    }
    if (processedFeed.length === 0) {
        return <p>최근 '진짜' P/S 거래가 없습니다.</p>;
    }
    return (
        <>
            <table className="feed-table">
                <thead>
                <tr>
                    <th>회사</th>
                    <th>유형</th>
                    <th>수량</th>
                    <th>단가</th>
                </tr>
                </thead>
                <tbody>
                {processedFeed.map((trade) => (
                    <tr key={trade.id}>
                        <td>{trade.ticker}</td>
                        <td className={trade.type.includes('Buy') ? 'positive' : 'negative'}>
                            {trade.type}
                        </td>
                        <td>{trade.shares.toLocaleString()}</td>
                        <td>${trade.price.toFixed(2)}</td>
                    </tr>
                ))}
                </tbody>
            </table>
        </>
    );
}

export default App;