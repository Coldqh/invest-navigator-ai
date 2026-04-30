import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { backendClient } from "../api/backendClient";
import { LoadingBlock } from "../components/LoadingBlock";
import type {
    AnalyticsSummaryResponse,
    AssetResponse,
    WatchlistItemResponse,
    WatchlistRefreshResponse,
} from "../types/api";

type WatchlistAnalyticsMap = Record<string, AnalyticsSummaryResponse | null>;

export function WatchlistPage() {
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [watchlist, setWatchlist] = useState<WatchlistItemResponse[]>([]);
    const [analyticsMap, setAnalyticsMap] = useState<WatchlistAnalyticsMap>({});
    const [lastRefresh, setLastRefresh] = useState<WatchlistRefreshResponse | null>(
        null
    );
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshingWatchlist, setIsRefreshingWatchlist] = useState(false);
    const [isUpdatingTicker, setIsUpdatingTicker] = useState<string | null>(null);
    const [error, setError] = useState("");

    const watchlistTickers = useMemo(() => {
        return new Set(watchlist.map((item) => item.ticker));
    }, [watchlist]);

    useEffect(() => {
        loadPage();
    }, []);

    async function loadPage() {
        try {
            setIsLoading(true);
            setError("");

            const [loadedAssets, loadedWatchlist] = await Promise.all([
                backendClient.getAssets(),
                backendClient.getWatchlist(),
            ]);

            setAssets(loadedAssets);
            setWatchlist(loadedWatchlist);

            await loadAnalytics(loadedWatchlist);
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось загрузить избранное");
        } finally {
            setIsLoading(false);
        }
    }

    async function loadAnalytics(items: WatchlistItemResponse[]) {
        const analyticsResults = await Promise.allSettled(
            items.map((item) => backendClient.getAnalyticsSummary(item.ticker))
        );

        const nextAnalyticsMap: WatchlistAnalyticsMap = {};

        analyticsResults.forEach((result, index) => {
            const ticker = items[index]?.ticker;

            if (!ticker) {
                return;
            }

            nextAnalyticsMap[ticker] =
                result.status === "fulfilled" ? result.value : null;
        });

        setAnalyticsMap(nextAnalyticsMap);
    }

    async function handleRefreshWatchlist() {
        setIsRefreshingWatchlist(true);
        setError("");

        try {
            const refreshResult = await backendClient.refreshWatchlist();
            setLastRefresh(refreshResult);

            const updatedWatchlist = await backendClient.getWatchlist();
            setWatchlist(updatedWatchlist);
            await loadAnalytics(updatedWatchlist);
        } catch (error: unknown) {
            setError(
                error instanceof Error ? error.message : "Не удалось обновить избранное"
            );
        } finally {
            setIsRefreshingWatchlist(false);
        }
    }

    async function handleAdd(ticker: string) {
        setIsUpdatingTicker(ticker);
        setError("");

        try {
            await backendClient.addToWatchlist(ticker);
            const updatedWatchlist = await backendClient.getWatchlist();

            setWatchlist(updatedWatchlist);
            await loadAnalytics(updatedWatchlist);
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось добавить актив");
        } finally {
            setIsUpdatingTicker(null);
        }
    }

    async function handleRemove(ticker: string) {
        setIsUpdatingTicker(ticker);
        setError("");

        try {
            await backendClient.removeFromWatchlist(ticker);
            const updatedWatchlist = await backendClient.getWatchlist();

            setWatchlist(updatedWatchlist);
            await loadAnalytics(updatedWatchlist);
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось убрать актив");
        } finally {
            setIsUpdatingTicker(null);
        }
    }

    if (isLoading) {
        return <LoadingBlock text="Загружаем избранное..." />;
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Избранное</p>
                    <h1>Избранные активы</h1>
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
                </div>
            </div>

            {error && <div className="error-block">{error}</div>}

            {lastRefresh && (
                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>Последнее обновление</h2>
                            <p>
                                Обновлено: {lastRefresh.refreshedItems} / {lastRefresh.totalItems}.
                                Ошибок: {lastRefresh.failedItems}.
                            </p>
                        </div>
                    </div>

                    <div className="risk-leaderboard">
                        {lastRefresh.items.map((item) => (
                            <div className="risk-leaderboard-row" key={item.ticker}>
                                <span>{item.ticker}</span>

                                <div>
                                    <strong>{item.refreshed ? "Обновлено" : "Ошибка"}</strong>
                                    <small>
                                        {item.refreshed
                                            ? `${item.source} · ${formatNumber(item.price)}`
                                            : item.errorMessage ?? "Неизвестная ошибка"}
                                    </small>
                                </div>

                                <span
                                    className={
                                        item.refreshed ? "risk risk-low" : "risk risk-critical"
                                    }
                                >
                                    {item.refreshed ? "OK" : "ERROR"}
                                </span>
                            </div>
                        ))}
                    </div>
                </article>
            )}

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Моё избранное</h2>
                        <p>Активов в списке: {watchlist.length}</p>
                    </div>
                </div>

                {watchlist.length === 0 ? (
                    <div className="empty-state">
                        <h3>Список пока пуст</h3>
                    </div>
                ) : (
                    <div className="watchlist-grid">
                        {watchlist.map((item) => {
                            const analytics = analyticsMap[item.ticker];

                            return (
                                <article className="watchlist-card" key={item.id}>
                                    <div>
                                        <p className="eyebrow">{item.assetType}</p>
                                        <h3>{item.ticker}</h3>
                                        <p>{item.name}</p>
                                    </div>

                                    <div className="watchlist-card-metrics">
                                        <span>{item.exchange}</span>
                                        <span>{item.currency}</span>
                                        <span>{analytics?.currentPrice ?? "—"}</span>
                                        <span
                                            className={`risk risk-${
                                                analytics?.riskLevel?.toLowerCase() ?? "unknown"
                                            }`}
                                        >
                                            {analytics?.riskLevel ?? "—"}
                                        </span>
                                    </div>

                                    <div className="watchlist-card-actions">
                                        <Link to={`/assets/${item.ticker}`} className="primary-button">
                                            Открыть
                                        </Link>

                                        <button
                                            type="button"
                                            className="ghost-button"
                                            disabled={isUpdatingTicker === item.ticker}
                                            onClick={() => handleRemove(item.ticker)}
                                        >
                                            {isUpdatingTicker === item.ticker ? "Убираем..." : "Убрать"}
                                        </button>
                                    </div>
                                </article>
                            );
                        })}
                    </div>
                )}
            </article>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Добавить актив</h2>
                    </div>
                </div>

                <div className="compare-picker">
                    {assets.map((asset) => {
                        const isInWatchlist = watchlistTickers.has(asset.ticker);
                        const isUpdating = isUpdatingTicker === asset.ticker;

                        return (
                            <button
                                type="button"
                                key={asset.id}
                                className={
                                    isInWatchlist
                                        ? "compare-chip compare-chip-active"
                                        : "compare-chip"
                                }
                                disabled={isUpdating}
                                onClick={() =>
                                    isInWatchlist
                                        ? handleRemove(asset.ticker)
                                        : handleAdd(asset.ticker)
                                }
                            >
                                <strong>{asset.ticker}</strong>
                                <span>
                                    {isUpdating
                                        ? "Обновляем..."
                                        : isInWatchlist
                                            ? "В избранном"
                                            : asset.exchange}
                                </span>
                            </button>
                        );
                    })}
                </div>
            </article>
        </section>
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