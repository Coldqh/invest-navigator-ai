export type AssetType =
    | "STOCK"
    | "BOND"
    | "ETF"
    | "INDEX"
    | "CURRENCY"
    | "CRYPTO";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

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