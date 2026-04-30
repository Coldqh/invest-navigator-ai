import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { backendClient } from "../api/backendClient";
import { LoadingBlock } from "../components/LoadingBlock";
import type {
    AIProviderHealthResponse,
    AIProviderHealthStatus,
    AIReportResponse,
    AnalyticsSummaryResponse,
    AssetResponse,
    MarketDataProviderHealthResponse,
    ProviderHealthStatus,
    WatchlistItemResponse,
    WatchlistRefreshResponse,
} from "../types/api";

type DashboardAsset = {
    asset: AssetResponse;
    analytics: AnalyticsSummaryResponse | null;
};

export function DashboardPage() {
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [analytics, setAnalytics] = useState<AnalyticsSummaryResponse[]>([]);
    const [reports, setReports] = useState<AIReportResponse[]>([]);
    const [watchlist, setWatchlist] = useState<WatchlistItemResponse[]>([]);
    const [lastWatchlistRefresh, setLastWatchlistRefresh] =
        useState<WatchlistRefreshResponse | null>(null);
    const [providerHealth, setProviderHealth] =
        useState<MarketDataProviderHealthResponse | null>(null);
    const [aiProviderHealth, setAIProviderHealth] =
        useState<AIProviderHealthResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshingWatchlist, setIsRefreshingWatchlist] = useState(false);
    const [error, setError] = useState("");

    const dashboardAssets = useMemo<DashboardAsset[]>(() => {
        return assets.map((asset) => {
            const assetAnalytics =
                analytics.find((item) => item.ticker === asset.ticker) ?? null;

            return {
                asset,
                analytics: assetAnalytics,
            };
        });
    }, [assets, analytics]);

    const watchlistAssets = useMemo(() => {
        return watchlist.map((watchlistItem) => {
            const assetAnalytics =
                analytics.find((item) => item.ticker === watchlistItem.ticker) ?? null;

            return {
                item: watchlistItem,
                analytics: assetAnalytics,
            };
        });
    }, [watchlist, analytics]);

    const highRiskAssets = useMemo(() => {
        return [...dashboardAssets]
            .filter((item) => item.analytics !== null)
            .sort((left, right) => {
                const leftRisk = left.analytics?.riskScore ?? 0;
                const rightRisk = right.analytics?.riskScore ?? 0;

                return rightRisk - leftRisk;
            })
            .slice(0, 3);
    }, [dashboardAssets]);

    useEffect(() => {
        async function loadDashboard() {
            try {
                setIsLoading(true);
                setError("");

                const loadedProviderHealth =
                    await backendClient.getMarketDataProviderHealth();
                setProviderHealth(loadedProviderHealth);

                const loadedAIProviderHealth = await backendClient.getAIProviderHealth();
                setAIProviderHealth(loadedAIProviderHealth);

                const [loadedAssets, loadedWatchlist] = await Promise.all([
                    backendClient.getAssets(),
                    backendClient.getWatchlist(),
                ]);

                setAssets(loadedAssets);
                setWatchlist(loadedWatchlist);

                const analyticsResults = await Promise.allSettled(
                    loadedAssets.map((asset) =>
                        backendClient.getAnalyticsSummary(asset.ticker)
                    )
                );

                const loadedAnalytics = analyticsResults
                    .filter(
                        (
                            result
                        ): result is PromiseFulfilledResult<AnalyticsSummaryResponse> =>
                            result.status === "fulfilled"
                    )
                    .map((result) => result.value);

                setAnalytics(loadedAnalytics);

                const reportResults = await Promise.allSettled(
                    loadedAssets.map((asset) => backendClient.getAIReports(asset.ticker))
                );

                const loadedReports = reportResults
                    .filter(
                        (result): result is PromiseFulfilledResult<AIReportResponse[]> =>
                            result.status === "fulfilled"
                    )
                    .flatMap((result) => result.value)
                    .sort((left, right) => {
                        return (
                            new Date(right.createdAt).getTime() -
                            new Date(left.createdAt).getTime()
                        );
                    })
                    .slice(0, 5);

                setReports(loadedReports);
            } catch (error: unknown) {
                setError(
                    error instanceof Error
                        ? error.message
                        : "Не удалось загрузить дашборд"
                );
            } finally {
                setIsLoading(false);
            }
        }

        loadDashboard();
    }, []);

    async function handleRefreshWatchlist() {
        setIsRefreshingWatchlist(true);
        setError("");

        try {
            const refreshResult = await backendClient.refreshWatchlist();
            setLastWatchlistRefresh(refreshResult);

            const [updatedWatchlist, updatedAssets] = await Promise.all([
                backendClient.getWatchlist(),
                backendClient.getAssets(),
            ]);

            setWatchlist(updatedWatchlist);
            setAssets(updatedAssets);

            const analyticsResults = await Promise.allSettled(
                updatedAssets.map((asset) =>
                    backendClient.getAnalyticsSummary(asset.ticker)
                )
            );

            const updatedAnalytics = analyticsResults
                .filter(
                    (
                        result
                    ): result is PromiseFulfilledResult<AnalyticsSummaryResponse> =>
                        result.status === "fulfilled"
                )
                .map((result) => result.value);

            setAnalytics(updatedAnalytics);
        } catch (error: unknown) {
            setError(
                error instanceof Error
                    ? error.message
                    : "Не удалось обновить избранное"
            );
        } finally {
            setIsRefreshingWatchlist(false);
        }
    }

    if (isLoading) {
        return <LoadingBlock text="Собираем рыночную сводку..." />;
    }

    return (
        <section className="page">
            <div className="dashboard-hero">
                <div>
                    <p className="eyebrow">Дашборд</p>
                    <h1>ИнвестНавигатор ИИ</h1>
                </div>

                <div className="hero-actions">
                    <Link to="/assets" className="primary-button">
                        Открыть активы
                    </Link>
                    <Link to="/watchlist" className="ghost-button">
                        Избранное
                    </Link>
                    <Link to="/compare" className="ghost-button">
                        Сравнить активы
                    </Link>
                </div>
            </div>

            {error && <div className="error-block">{error}</div>}

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Избранное</h2>
                    </div>

                    <div className="hero-actions">
                        <button
                            type="button"
                            className="primary-button"
                            disabled={isRefreshingWatchlist || watchlist.length === 0}
                            onClick={handleRefreshWatchlist}
                        >
                            {isRefreshingWatchlist ? "Обновляем..." : "Обновить избранное"}
                        </button>

                        <Link to="/watchlist" className="secondary-link">
                            Управлять →
                        </Link>
                    </div>
                </div>

                {lastWatchlistRefresh && (
                    <div className="empty-state">
                        Обновлено: {lastWatchlistRefresh.refreshedItems} /{" "}
                        {lastWatchlistRefresh.totalItems}. Ошибок:{" "}
                        {lastWatchlistRefresh.failedItems}.
                    </div>
                )}

                {watchlistAssets.length === 0 ? (
                    <div className="empty-state">
                        <h3>Избранное пока пустое</h3>
                        <Link to="/watchlist" className="primary-button">
                            Добавить активы
                        </Link>
                    </div>
                ) : (
                    <div className="dashboard-asset-list">
                        {watchlistAssets.map(({ item, analytics }) => (
                            <Link
                                to={`/assets/${item.ticker}`}
                                className="dashboard-asset-card"
                                key={item.id}
                            >
                                <div>
                                    <strong>{item.ticker}</strong>
                                    <span>{item.name}</span>
                                </div>

                                <div className="dashboard-asset-metrics">
                                    <span>{item.exchange}</span>
                                    <span>{item.assetType}</span>
                                    <span>{analytics?.currentPrice ?? "—"}</span>
                                    <span
                                        className={`risk risk-${
                                            analytics?.riskLevel?.toLowerCase() ?? "unknown"
                                        }`}
                                    >
                                        {analytics?.riskLevel ?? "—"}
                                    </span>
                                </div>
                            </Link>
                        ))}
                    </div>
                )}
            </article>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Состояние провайдеров данных</h2>
                    </div>
                </div>

                {providerHealth ? (
                    <div className="risk-leaderboard">
                        {providerHealth.providers.map((provider) => (
                            <div className="risk-leaderboard-row" key={provider.type}>
                                <span>{provider.type}</span>

                                <div>
                                    <strong>{provider.status}</strong>
                                    <small>{provider.message}</small>
                                </div>

                                <span className={getProviderStatusClass(provider.status)}>
                                    {provider.status}
                                </span>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="empty-state">
                        <h3>Нет данных</h3>
                    </div>
                )}
            </article>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Состояние AI-провайдеров</h2>
                    </div>
                </div>

                {aiProviderHealth ? (
                    <div className="risk-leaderboard">
                        {aiProviderHealth.providers.map((provider) => (
                            <div className="risk-leaderboard-row" key={provider.type}>
                                <span>{provider.type}</span>

                                <div>
                                    <strong>{provider.status}</strong>
                                    <small>{provider.message}</small>
                                </div>

                                <span className={getProviderStatusClass(provider.status)}>
                                    {provider.status}
                                </span>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="empty-state">
                        <h3>Нет данных</h3>
                    </div>
                )}
            </article>

            <div className="dashboard-grid">
                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>Рыночные карточки</h2>
                        </div>

                        <Link to="/assets" className="secondary-link">
                            Все активы →
                        </Link>
                    </div>

                    <div className="dashboard-asset-list">
                        {dashboardAssets.map((item) => (
                            <Link
                                to={`/assets/${item.asset.ticker}`}
                                className="dashboard-asset-card"
                                key={item.asset.id}
                            >
                                <div>
                                    <strong>{item.asset.ticker}</strong>
                                    <span>{item.asset.name}</span>
                                </div>

                                <div className="dashboard-asset-metrics">
                                    <span>{item.asset.exchange}</span>
                                    <span>{item.analytics?.currentPrice ?? "—"}</span>
                                    <span
                                        className={`risk risk-${
                                            item.analytics?.riskLevel?.toLowerCase() ?? "unknown"
                                        }`}
                                    >
                                        {item.analytics?.riskLevel ?? "—"}
                                    </span>
                                </div>
                            </Link>
                        ))}
                    </div>
                </article>

                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>Топ риска</h2>
                        </div>
                    </div>

                    <div className="risk-leaderboard">
                        {highRiskAssets.map((item, index) => (
                            <Link
                                to={`/assets/${item.asset.ticker}`}
                                className="risk-leaderboard-row"
                                key={item.asset.id}
                            >
                                <span>#{index + 1}</span>

                                <div>
                                    <strong>{item.asset.ticker}</strong>
                                    <small>{item.asset.name}</small>
                                </div>

                                <strong>{item.analytics?.riskScore ?? "—"} / 100</strong>
                            </Link>
                        ))}
                    </div>
                </article>
            </div>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Последние AI-отчёты</h2>
                    </div>
                </div>

                {reports.length === 0 ? (
                    <div className="empty-state">
                        <h3>Отчётов пока нет</h3>
                        <Link to="/assets/SBER" className="primary-button">
                            Создать первый отчёт
                        </Link>
                    </div>
                ) : (
                    <div className="recent-reports">
                        {reports.map((report) => (
                            <Link
                                to={`/assets/${report.ticker}`}
                                className="recent-report-card"
                                key={report.id}
                            >
                                <div className="report-card-header">
                                    <strong>{report.ticker}</strong>
                                    <span>{formatDate(report.createdAt)}</span>
                                </div>

                                <p>{report.summary}</p>

                                <div className="recent-report-footer">
                                    <span className={`risk risk-${report.riskLevel.toLowerCase()}`}>
                                        {report.riskLevel}
                                    </span>
                                    <span>{report.provider}</span>
                                    <span>{report.confidence}</span>
                                </div>
                            </Link>
                        ))}
                    </div>
                )}
            </article>
        </section>
    );
}

function getProviderStatusClass(
    status: ProviderHealthStatus | AIProviderHealthStatus
): string {
    if (status === "AVAILABLE") {
        return "risk risk-low";
    }

    if (status === "DEGRADED") {
        return "risk risk-medium";
    }

    if (status === "UNAVAILABLE") {
        return "risk risk-critical";
    }

    return "risk risk-unknown";
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