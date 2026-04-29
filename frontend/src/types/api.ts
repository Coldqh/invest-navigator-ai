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
    summary: string;
    positiveFactors: string[];
    negativeFactors: string[];
    riskLevel: RiskLevel;
    riskScore: number;
    confidence: number;
    explanation: string;
    disclaimer: string;
    createdAt: string;
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