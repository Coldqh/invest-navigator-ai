export type AssetType =
    | "STOCK"
    | "BOND"
    | "ETF"
    | "INDEX"
    | "CURRENCY"
    | "CRYPTO";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type MarketDataProviderType =
    | "DEMO"
    | "BINANCE"
    | "MOEX"
    | "T_INVEST"
    | "HYBRID";

export type ProviderHealthStatus =
    | "AVAILABLE"
    | "DEGRADED"
    | "UNAVAILABLE"
    | "NOT_CONFIGURED";

export type AIProviderType =
    | "MOCK"
    | "YANDEX_GPT"
    | "GIGA_CHAT"
    | "OPENAI";

export type AIProviderHealthStatus =
    | "AVAILABLE"
    | "DEGRADED"
    | "UNAVAILABLE"
    | "NOT_CONFIGURED";

export type AssetResponse = {
    id: string;
    ticker: string;
    name: string;
    assetType: AssetType;
    exchange: string;
    currency: string;
    isin: string | null;
    active: boolean;
};

export type MarketPriceResponse = {
    assetId: string;
    ticker: string;
    name: string;
    price: number;
    volume: number;
    source: string;
    timestamp: string;
};

export type CandleResponse = {
    timeframe: string;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
    source: string;
    timestamp: string;
};

export type AnalyticsSummaryResponse = {
    ticker: string;
    name: string;
    currentPrice: number;
    firstClose: number;
    lastClose: number;
    priceChange: number;
    priceChangePercent: number;
    averageVolume: number;
    volatilityPercent: number;
    riskScore: number;
    riskLevel: RiskLevel;
    dataPoints: number;
};

export type AIReportResponse = {
    id: string;
    assetId: string;
    ticker: string;
    name: string;
    provider: AIProviderType;
    summary: string;
    positiveFactors: string[];
    negativeFactors: string[];
    riskLevel: RiskLevel;
    riskScore: number;
    confidence: number;
    explanation: string;
    disclaimer: string;
    fallbackReason?: string | null;
    createdAt: string;
};

export type WatchlistItemResponse = {
    id: string;
    assetId: string;
    ticker: string;
    name: string;
    assetType: AssetType;
    exchange: string;
    currency: string;
    isin: string | null;
    active: boolean;
    createdAt: string;
};

export type WatchlistRefreshItemResponse = {
    ticker: string;
    name: string;
    refreshed: boolean;
    price: number | null;
    volume: number | null;
    source: string | null;
    timestamp: string | null;
    errorMessage: string | null;
};

export type WatchlistRefreshResponse = {
    totalItems: number;
    refreshedItems: number;
    failedItems: number;
    items: WatchlistRefreshItemResponse[];
    refreshedAt: string;
};

export type AIWatchlistItemSnapshot = {
    ticker: string;
    name: string;
    assetType: AssetType;
    exchange: string;
    currency: string;
    latestPrice: number | null;
    latestVolume: number | null;
    priceSource: string | null;
    priceTimestamp: string | null;
    dataError: string | null;
};

export type AIWatchlistReportResponse = {
    provider: AIProviderType;
    itemsCount: number;
    items: AIWatchlistItemSnapshot[];
    summary: string;
    positiveFactors: string[];
    negativeFactors: string[];
    riskLevel: RiskLevel;
    riskScore: number;
    confidence: number;
    explanation: string;
    disclaimer: string;
    fallbackReason: string | null;
    generatedAt: string;
};

export type PortfolioPositionRequest = {
    ticker: string;
    quantity: number;
    averageBuyPrice: number;
};

export type PortfolioPositionResponse = {
    id: string;
    assetId: string;
    ticker: string;
    name: string;
    assetType: AssetType;
    exchange: string;
    currency: string;
    quantity: number;
    averageBuyPrice: number;
    investedAmount: number;
    currentPrice: number;
    currentValue: number;
    profitLoss: number;
    profitLossPercent: number;
    priceSource: string;
    priceTimestamp: string;
    createdAt: string;
    updatedAt: string;
};

export type PortfolioSummaryResponse = {
    positionsCount: number;
    totalInvested: number;
    totalCurrentValue: number;
    totalProfitLoss: number;
    totalProfitLossPercent: number;
    positions: PortfolioPositionResponse[];
    calculatedAt: string;
};

export type AIPortfolioReportResponse = {
    provider: AIProviderType;
    positionsCount: number;
    totalInvested: number;
    totalCurrentValue: number;
    totalProfitLoss: number;
    totalProfitLossPercent: number;
    summary: string;
    positiveFactors: string[];
    negativeFactors: string[];
    riskLevel: RiskLevel;
    riskScore: number;
    confidence: number;
    explanation: string;
    disclaimer: string;
    fallbackReason: string | null;
    generatedAt: string;
};

export type ApiErrorResponse = {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
};

export type MarketDataProviderStatusResponse = {
    activeProvider: MarketDataProviderType;
    status: string;
};

export type MarketDataProviderHealthItemResponse = {
    type: MarketDataProviderType;
    status: ProviderHealthStatus;
    message: string;
    checkedAt: string;
};

export type MarketDataProviderHealthResponse = {
    activeProvider: MarketDataProviderType;
    status: ProviderHealthStatus;
    providers: MarketDataProviderHealthItemResponse[];
    checkedAt: string;
};

export type AIProviderStatusResponse = {
    activeProvider: AIProviderType;
    status: string;
};

export type AIProviderHealthItemResponse = {
    type: AIProviderType;
    status: AIProviderHealthStatus;
    message: string;
    checkedAt: string;
};

export type AIProviderHealthResponse = {
    activeProvider: AIProviderType;
    status: AIProviderHealthStatus;
    providers: AIProviderHealthItemResponse[];
    checkedAt: string;
};