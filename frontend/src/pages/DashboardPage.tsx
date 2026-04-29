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
} from "../types/api";

type DashboardAsset = {
    asset: AssetResponse;
    analytics: AnalyticsSummaryResponse | null;
};

const riskWeight: Record<string, number> = {
    LOW: 1,
    MEDIUM: 2,
    HIGH: 3,
    CRITICAL: 4,
};

export function DashboardPage() {
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [analytics, setAnalytics] = useState<AnalyticsSummaryResponse[]>([]);
    const [reports, setReports] = useState<AIReportResponse[]>([]);
    const [watchlist, setWatchlist] = useState<WatchlistItemResponse[]>([]);
    const [providerHealth, setProviderHealth] =
        useState<MarketDataProviderHealthResponse | null>(null);
    const [aiProviderHealth, setAIProviderHealth] =
        useState<AIProviderHealthResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
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

    const riskOverview = useMemo(() => {
        const totalRiskScore = analytics.reduce(
            (sum, item) => sum + item.riskScore,
            0
        );

        const averageRiskScore =
            analytics.length > 0 ? Math.round(totalRiskScore / analytics.length) : 0;

        const mostCommonRiskLevel = analytics
            .map((item) => item.riskLevel)
            .sort((left, right) => {
                return (riskWeight[right] ?? 0) - (riskWeight[left] ?? 0);
            })[0];

        return {
            totalAssets: assets.length,
            analyticsCount: analytics.length,
            watchlistCount: watchlist.length,
            averageRiskScore,
            mostCommonRiskLevel: mostCommonRiskLevel ?? "—",
        };
    }, [assets.length, analytics, watchlist.length]);

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

    if (isLoading) {
        return <LoadingBlock text="Собираем рыночную сводку..." />;
    }

    return (
        <section className="page">
            <div className="dashboard-hero">
                <div>
                    <p className="eyebrow">Dashboard</p>
                    <h1>ИнвестНавигатор ИИ</h1>
                    <p>
                        Главная панель проекта: активы, риск, избранные инструменты,
                        провайдеры данных, AI-провайдеры и последние AI-отчёты.
                    </p>
                </div>

                <div className="hero-actions">
                    <Link to="/assets" className="primary-button">
                        Открыть активы
                    </Link>
                    <Link to="/watchlist" className="ghost-button">
                        Watchlist
                    </Link>
                    <Link to="/compare" className="ghost-button">
                        Сравнить активы
                    </Link>
                </div>
            </div>

            {error && <div className="error-block">{error}</div>}

            <div className="dashboard-stats">
                <article className="dashboard-stat-card">
                    <span>Активов</span>
                    <strong>{riskOverview.totalAssets}</strong>
                    <p>Доступно для анализа</p>
                </article>

                <article className="dashboard-stat-card">
                    <span>В watchlist</span>
                    <strong>{riskOverview.watchlistCount}</strong>
                    <p>Избранные инструменты</p>
                </article>

                <article className="dashboard-stat-card">
                    <span>С аналитикой</span>
                    <strong>{riskOverview.analyticsCount}</strong>
                    <p>Есть свечи и risk score</p>
                </article>

                <article className="dashboard-stat-card">
                    <span>Средний риск</span>
                    <strong>{riskOverview.averageRiskScore}</strong>
                    <p>Средний risk score</p>
                </article>

                <article className="dashboard-stat-card">
                    <span>Макс. уровень</span>
                    <strong>{riskOverview.mostCommonRiskLevel}</strong>
                    <p>Самый высокий риск среди активов</p>
                </article>

                <article className="dashboard-stat-card provider-status-card">
                    <span>Источник данных</span>
                    <strong>{providerHealth?.activeProvider ?? "—"}</strong>
                    <p>{providerHealth?.status ?? "UNKNOWN"}</p>
                </article>

                <article className="dashboard-stat-card provider-status-card">
                    <span>AI-провайдер</span>
                    <strong>{aiProviderHealth?.activeProvider ?? "—"}</strong>
                    <p>{aiProviderHealth?.status ?? "UNKNOWN"}</p>
                </article>
            </div>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Watchlist</h2>
                        <p>Избранные активы с текущей ценой и уровнем риска.</p>
                    </div>

                    <Link to="/watchlist" className="secondary-link">
                        Управлять →
                    </Link>
                </div>

                {watchlistAssets.length === 0 ? (
                    <div className="empty-state">
                        <h3>Watchlist пока пуст</h3>
                        <p>
                            Добавь SBER, BTCUSDT или другой актив, чтобы видеть его прямо на
                            главной странице.
                        </p>
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
                        <p>Проверка доступности источников рыночных данных.</p>
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
                    <p>Информация о провайдерах данных недоступна.</p>
                )}
            </article>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Состояние AI-провайдеров</h2>
                        <p>Проверка доступности источников AI-анализа.</p>
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
                    <p>Информация об AI-провайдерах недоступна.</p>
                )}
            </article>

            <div className="dashboard-grid">
                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>Рыночные карточки</h2>
                            <p>Краткая сводка по активам из backend.</p>
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
                            <p>Активы с самым высоким risk score.</p>
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
                        <p>Недавние отчёты, сохранённые в базе.</p>
                    </div>
                </div>

                {reports.length === 0 ? (
                    <div className="empty-state">
                        <h3>Отчётов пока нет</h3>
                        <p>
                            Открой карточку любого актива и нажми «Создать отчёт». После
                            этого отчёты появятся здесь.
                        </p>
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
                                    <span>{new Date(report.createdAt).toLocaleString("ru-RU")}</span>
                                </div>

                                <p>{report.summary}</p>

                                <div className="recent-report-footer">
                  <span className={`risk risk-${report.riskLevel.toLowerCase()}`}>
                    {report.riskLevel}
                  </span>
                                    <span>{report.provider}</span>
                                    <span>{report.confidence} confidence</span>
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