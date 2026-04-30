import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { backendClient } from "../api/backendClient";
import { AIReportCard } from "../components/AIReportCard";
import { LoadingBlock } from "../components/LoadingBlock";
import type {
    AIReportResponse,
    AnalyticsSummaryResponse,
    AssetResponse,
    CandleResponse,
    MarketPriceResponse,
    WatchlistItemResponse,
} from "../types/api";

export function AssetDetailsPage() {
    const { ticker } = useParams<{ ticker: string }>();

    const [asset, setAsset] = useState<AssetResponse | null>(null);
    const [marketData, setMarketData] = useState<MarketPriceResponse | null>(null);
    const [candles, setCandles] = useState<CandleResponse[]>([]);
    const [analytics, setAnalytics] = useState<AnalyticsSummaryResponse | null>(
        null
    );
    const [reports, setReports] = useState<AIReportResponse[]>([]);
    const [watchlist, setWatchlist] = useState<WatchlistItemResponse[]>([]);

    const [isLoading, setIsLoading] = useState(true);
    const [isGeneratingReport, setIsGeneratingReport] = useState(false);
    const [isRefreshingMarketData, setIsRefreshingMarketData] = useState(false);
    const [isUpdatingWatchlist, setIsUpdatingWatchlist] = useState(false);

    const [error, setError] = useState("");
    const [warning, setWarning] = useState("");

    const reportGenerationLockRef = useRef(false);

    const normalizedTicker = ticker?.toUpperCase() ?? "";

    const isInWatchlist = watchlist.some(
        (item) => item.ticker.toUpperCase() === normalizedTicker
    );

    useEffect(() => {
        async function loadAssetDetails() {
            if (!normalizedTicker) {
                setError("Не указан тикер");
                setIsLoading(false);
                return;
            }

            try {
                setIsLoading(true);
                setError("");
                setWarning("");

                const [
                    loadedAsset,
                    loadedMarketData,
                    loadedCandles,
                    loadedAnalytics,
                    loadedReports,
                    loadedWatchlist,
                ] = await Promise.all([
                    backendClient.getAsset(normalizedTicker),
                    backendClient.getMarketData(normalizedTicker),
                    backendClient.getCandles(normalizedTicker),
                    backendClient.getAnalyticsSummary(normalizedTicker),
                    backendClient.getAIReports(normalizedTicker),
                    backendClient.getWatchlist(),
                ]);

                setAsset(loadedAsset);
                setMarketData(loadedMarketData);
                setCandles(loadedCandles);
                setAnalytics(loadedAnalytics);
                setReports(loadedReports);
                setWatchlist(loadedWatchlist);
            } catch (error: unknown) {
                setError(
                    error instanceof Error
                        ? error.message
                        : "Не удалось загрузить карточку актива"
                );
            } finally {
                setIsLoading(false);
            }
        }

        loadAssetDetails();
    }, [normalizedTicker]);

    async function handleRefreshMarketData() {
        if (!normalizedTicker) {
            return;
        }

        setIsRefreshingMarketData(true);
        setError("");
        setWarning("");

        try {
            const refreshedMarketData =
                await backendClient.refreshMarketData(normalizedTicker);
            setMarketData(refreshedMarketData);

            const [refreshedAnalytics, refreshedCandles] = await Promise.all([
                backendClient.getAnalyticsSummary(normalizedTicker),
                backendClient.getCandles(normalizedTicker),
            ]);

            setAnalytics(refreshedAnalytics);
            setCandles(refreshedCandles);
        } catch (error: unknown) {
            setError(
                error instanceof Error
                    ? error.message
                    : "Не удалось обновить рыночные данные"
            );
        } finally {
            setIsRefreshingMarketData(false);
        }
    }

    async function handleGenerateReport() {
        if (!normalizedTicker || reportGenerationLockRef.current) {
            return;
        }

        reportGenerationLockRef.current = true;
        setIsGeneratingReport(true);
        setError("");
        setWarning("");

        try {
            const newReport = await backendClient.generateAIReport(normalizedTicker);
            setReports([newReport]);
        } catch (error: unknown) {
            setError(
                error instanceof Error ? error.message : "Не удалось создать AI-отчёт"
            );
        } finally {
            reportGenerationLockRef.current = false;
            setIsGeneratingReport(false);
        }
    }

    async function handleToggleWatchlist() {
        if (!normalizedTicker) {
            return;
        }

        setIsUpdatingWatchlist(true);
        setError("");
        setWarning("");

        try {
            if (isInWatchlist) {
                await backendClient.removeFromWatchlist(normalizedTicker);

                setWatchlist((currentWatchlist) =>
                    currentWatchlist.filter(
                        (item) => item.ticker.toUpperCase() !== normalizedTicker
                    )
                );

                setWarning(`${normalizedTicker} убран из избранного`);
                return;
            }

            const addedItem = await backendClient.addToWatchlist(normalizedTicker);

            setWatchlist((currentWatchlist) => {
                const alreadyExists = currentWatchlist.some(
                    (item) => item.ticker.toUpperCase() === normalizedTicker
                );

                if (alreadyExists) {
                    return currentWatchlist;
                }

                return [addedItem, ...currentWatchlist];
            });

            setWarning(`${normalizedTicker} добавлен в избранное`);
        } catch (error: unknown) {
            setError(
                error instanceof Error
                    ? error.message
                    : "Не удалось обновить избранное"
            );
        } finally {
            setIsUpdatingWatchlist(false);
        }
    }

    if (isLoading) {
        return <LoadingBlock text="Загружаем карточку актива..." />;
    }

    if (!asset) {
        return (
            <section className="page">
                <div className="error-block">
                    {error || "Актив не найден"}
                </div>

                <Link to="/assets" className="primary-button">
                    Вернуться к активам
                </Link>
            </section>
        );
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">{asset.assetType}</p>
                    <h1>{asset.ticker}</h1>
                    <p>{asset.name}</p>
                </div>

                <div className="asset-header-side">
                    <div className="badge-row">
                        <span>{asset.exchange}</span>
                        <span>{asset.currency}</span>
                    </div>

                    <div className="hero-actions">
                        <button
                            type="button"
                            className={isInWatchlist ? "primary-button" : "ghost-button"}
                            disabled={isUpdatingWatchlist}
                            onClick={handleToggleWatchlist}
                        >
                            {isUpdatingWatchlist
                                ? "Обновляем..."
                                : isInWatchlist
                                    ? "В избранном"
                                    : "Добавить в избранное"}
                        </button>

                        <button
                            type="button"
                            className="ghost-button"
                            disabled={isRefreshingMarketData}
                            onClick={handleRefreshMarketData}
                        >
                            {isRefreshingMarketData ? "Обновляем..." : "Обновить цену"}
                        </button>

                        <Link to="/watchlist" className="ghost-button">
                            Открыть избранное
                        </Link>
                    </div>
                </div>
            </div>

            {error && <div className="error-block">{error}</div>}
            {warning && <div className="empty-state">{warning}</div>}

            <div className="dashboard-stats">
                <article className="dashboard-stat-card">
                    <span>Текущая цена</span>
                    <strong>{formatNumber(marketData?.price)}</strong>
                </article>

                <article className="dashboard-stat-card">
                    <span>Объём</span>
                    <strong>{formatNumber(marketData?.volume)}</strong>
                </article>

                <article className="dashboard-stat-card">
                    <span>Риск-скор</span>
                    <strong>{analytics?.riskScore ?? "—"}</strong>
                </article>

                <article className="dashboard-stat-card">
                    <span>Изменение</span>
                    <strong>{formatPercent(analytics?.priceChangePercent)}</strong>
                </article>

                <article className="dashboard-stat-card">
                    <span>Волатильность</span>
                    <strong>{formatPercent(analytics?.volatilityPercent)}</strong>
                </article>
            </div>

            <div className="dashboard-grid">
                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>График свечей</h2>
                        </div>
                    </div>

                    <InlineMiniChart candles={candles} />

                    <div className="dashboard-asset-list">
                        {candles.slice(-5).reverse().map((candle) => (
                            <div
                                className="dashboard-asset-card"
                                key={`${candle.timestamp}-${candle.close}`}
                            >
                                <div>
                                    <strong>{formatDate(candle.timestamp)}</strong>
                                    <span>{candle.source}</span>
                                </div>

                                <div className="dashboard-asset-metrics">
                                    <span>O {formatNumber(candle.open)}</span>
                                    <span>H {formatNumber(candle.high)}</span>
                                    <span>L {formatNumber(candle.low)}</span>
                                    <span>C {formatNumber(candle.close)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </article>

                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>Аналитика</h2>
                        </div>
                    </div>

                    {analytics ? (
                        <div className="dashboard-asset-list">
                            <MetricRow label="Первое закрытие" value={formatNumber(analytics.firstClose)} />
                            <MetricRow label="Последнее закрытие" value={formatNumber(analytics.lastClose)} />
                            <MetricRow label="Средний объём" value={formatNumber(analytics.averageVolume)} />
                            <MetricRow label="Точек данных" value={analytics.dataPoints.toString()} />
                            <MetricRow label="Уровень риска" value={analytics.riskLevel} />
                        </div>
                    ) : (
                        <div className="empty-state">
                            <h3>Аналитика недоступна</h3>
                        </div>
                    )}
                </article>
            </div>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>AI-отчёт</h2>
                    </div>

                    <button
                        type="button"
                        className="primary-button"
                        disabled={isGeneratingReport}
                        onClick={handleGenerateReport}
                    >
                        {isGeneratingReport ? "Создаём..." : "Создать отчёт"}
                    </button>
                </div>

                <div className="report-list">
                    {reports.length === 0 && (
                        <div className="empty-state">
                            <h3>Отчётов пока нет</h3>
                        </div>
                    )}

                    {reports.map((report) => (
                        <AIReportCard report={report} key={report.id} />
                    ))}
                </div>
            </article>
        </section>
    );
}

type MetricRowProps = {
    label: string;
    value: string;
};

function MetricRow({ label, value }: MetricRowProps) {
    return (
        <div className="dashboard-asset-card">
            <div>
                <strong>{label}</strong>
            </div>

            <div className="dashboard-asset-metrics">
                <span>{value}</span>
            </div>
        </div>
    );
}

type InlineMiniChartProps = {
    candles: CandleResponse[];
};

function InlineMiniChart({ candles }: InlineMiniChartProps) {
    if (candles.length < 2) {
        return (
            <div className="empty-state">
                <h3>Недостаточно свечей</h3>
            </div>
        );
    }

    const closes = candles.map((candle) => candle.close);
    const minClose = Math.min(...closes);
    const maxClose = Math.max(...closes);
    const width = 720;
    const height = 220;
    const padding = 18;
    const range = maxClose - minClose || 1;

    const points = closes
        .map((close, index) => {
            const x =
                padding +
                (index / Math.max(closes.length - 1, 1)) * (width - padding * 2);

            const y =
                height -
                padding -
                ((close - minClose) / range) * (height - padding * 2);

            return `${x},${y}`;
        })
        .join(" ");

    return (
        <div className="ai-report-summary">
            <svg
                viewBox={`0 0 ${width} ${height}`}
                role="img"
                aria-label="График цены закрытия"
                style={{ width: "100%", height: "220px" }}
            >
                <polyline
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="4"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points={points}
                />
            </svg>
        </div>
    );
}

function formatNumber(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "—";
    }

    return new Intl.NumberFormat("ru-RU", {
        maximumFractionDigits: 6,
    }).format(value);
}

function formatPercent(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "—";
    }

    return `${formatNumber(value)}%`;
}

function formatDate(value: string | null | undefined): string {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime()) || date.getFullYear() <= 1971) {
        return "—";
    }

    return date.toLocaleString("ru-RU");
}