import type {
    AICompareReportResponse,
    AIProviderHealthResponse,
    AIProviderStatusResponse,
    AIReportResponse,
    AIPortfolioReportResponse,
    AnalyticsSummaryResponse,
    ApiErrorResponse,
    AssetResponse,
    CandleResponse,
    MarketDataProviderHealthResponse,
    MarketDataProviderStatusResponse,
    MarketPriceResponse,
    PortfolioPositionRequest,
    PortfolioPositionResponse,
    PortfolioSummaryResponse,
    WatchlistItemResponse,
    WatchlistRefreshResponse,
} from "../types/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        headers: {
            "Content-Type": "application/json",
            ...(options?.headers ?? {}),
        },
        ...options,
    });

    if (!response.ok) {
        let message = `Request failed with status ${response.status}`;

        try {
            const errorBody = (await response.json()) as ApiErrorResponse;
            message = errorBody.message || message;
        } catch {
            // ignore parsing error
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}

export const backendClient = {
    getAssets(): Promise<AssetResponse[]> {
        return request<AssetResponse[]>("/api/assets");
    },

    getAsset(ticker: string): Promise<AssetResponse> {
        return request<AssetResponse>(`/api/assets/${ticker}`);
    },

    getMarketData(ticker: string): Promise<MarketPriceResponse> {
        return request<MarketPriceResponse>(`/api/assets/${ticker}/market-data`);
    },

    getCandles(ticker: string): Promise<CandleResponse[]> {
        return request<CandleResponse[]>(`/api/assets/${ticker}/candles?timeframe=1D`);
    },

    getAnalyticsSummary(ticker: string): Promise<AnalyticsSummaryResponse> {
        return request<AnalyticsSummaryResponse>(`/api/analytics/${ticker}/summary`);
    },

    generateAIReport(ticker: string): Promise<AIReportResponse> {
        return request<AIReportResponse>(`/api/ai/analyze/${ticker}`, {
            method: "POST",
        });
    },

    getAIReports(ticker: string): Promise<AIReportResponse[]> {
        return request<AIReportResponse[]>(`/api/ai/reports/${ticker}`);
    },

    compareAssets(tickers: string[]): Promise<AnalyticsSummaryResponse[]> {
        return request<AnalyticsSummaryResponse[]>("/api/analytics/compare", {
            method: "POST",
            body: JSON.stringify({ tickers }),
        });
    },

    generateAICompareReport(
        firstTicker: string,
        secondTicker: string
    ): Promise<AICompareReportResponse> {
        const first = encodeURIComponent(firstTicker);
        const second = encodeURIComponent(secondTicker);

        return request<AICompareReportResponse>(
            `/api/ai/compare/analyze?firstTicker=${first}&secondTicker=${second}`,
            {
                method: "POST",
            }
        );
    },

    refreshMarketData(ticker: string): Promise<MarketPriceResponse> {
        return request<MarketPriceResponse>(`/api/market-data/refresh/${ticker}`, {
            method: "POST",
        });
    },

    getMarketDataProviderStatus(): Promise<MarketDataProviderStatusResponse> {
        return request<MarketDataProviderStatusResponse>("/api/market-data/provider");
    },

    getMarketDataProviderHealth(): Promise<MarketDataProviderHealthResponse> {
        return request<MarketDataProviderHealthResponse>("/api/market-data/provider/health");
    },

    getAIProviderStatus(): Promise<AIProviderStatusResponse> {
        return request<AIProviderStatusResponse>("/api/ai/provider");
    },

    getAIProviderHealth(): Promise<AIProviderHealthResponse> {
        return request<AIProviderHealthResponse>("/api/ai/provider/health");
    },

    getWatchlist(): Promise<WatchlistItemResponse[]> {
        return request<WatchlistItemResponse[]>("/api/watchlist");
    },

    addToWatchlist(ticker: string): Promise<WatchlistItemResponse> {
        return request<WatchlistItemResponse>(`/api/watchlist/${ticker}`, {
            method: "POST",
        });
    },

    removeFromWatchlist(ticker: string): Promise<void> {
        return request<void>(`/api/watchlist/${ticker}`, {
            method: "DELETE",
        });
    },

    refreshWatchlist(): Promise<WatchlistRefreshResponse> {
        return request<WatchlistRefreshResponse>("/api/watchlist/refresh", {
            method: "POST",
        });
    },

    getPortfolio(): Promise<PortfolioSummaryResponse> {
        return request<PortfolioSummaryResponse>("/api/portfolio");
    },

    addPortfolioPosition(
        position: PortfolioPositionRequest
    ): Promise<PortfolioPositionResponse> {
        return request<PortfolioPositionResponse>("/api/portfolio/positions", {
            method: "POST",
            body: JSON.stringify(position),
        });
    },

    removePortfolioPosition(ticker: string): Promise<void> {
        return request<void>(`/api/portfolio/positions/${ticker}`, {
            method: "DELETE",
        });
    },

    generateAIPortfolioReport(): Promise<AIPortfolioReportResponse> {
        return request<AIPortfolioReportResponse>("/api/ai/portfolio/analyze", {
            method: "POST",
        });
    },
};