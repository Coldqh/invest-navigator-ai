import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import { backendClient } from "../api/backendClient";
import { LoadingBlock } from "../components/LoadingBlock";
import type {
    AIPortfolioReportResponse,
    AssetResponse,
    PortfolioSummaryResponse,
} from "../types/api";

type PortfolioFormState = {
    ticker: string;
    quantity: string;
    averageBuyPrice: string;
};

const initialFormState: PortfolioFormState = {
    ticker: "",
    quantity: "",
    averageBuyPrice: "",
};

export function PortfolioPage() {
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [assetSearch, setAssetSearch] = useState("");
    const [portfolio, setPortfolio] = useState<PortfolioSummaryResponse | null>(null);
    const [aiReport, setAiReport] = useState<AIPortfolioReportResponse | null>(null);
    const [formState, setFormState] = useState<PortfolioFormState>(initialFormState);
    const [isLoading, setIsLoading] = useState(true);
    const [isSavingPosition, setIsSavingPosition] = useState(false);
    const [isGeneratingReport, setIsGeneratingReport] = useState(false);
    const [deletingTicker, setDeletingTicker] = useState<string | null>(null);
    const [error, setError] = useState("");

    const hasPositions = Boolean(portfolio && portfolio.positionsCount > 0);

    const sortedPositions = useMemo(() => {
        if (!portfolio) {
            return [];
        }

        return [...portfolio.positions].sort((first, second) =>
            second.currentValue - first.currentValue
        );
    }, [portfolio]);

    const filteredAssets = useMemo(() => {
        const normalizedSearch = assetSearch.trim().toLowerCase();

        if (!normalizedSearch) {
            return assets;
        }

        return assets.filter((asset) => {
            return (
                asset.ticker.toLowerCase().includes(normalizedSearch) ||
                asset.name.toLowerCase().includes(normalizedSearch)
            );
        });
    }, [assets, assetSearch]);

    useEffect(() => {
        loadPage();
    }, []);

    async function loadPage() {
        try {
            setIsLoading(true);
            setError("");

            const [loadedAssets, loadedPortfolio] = await Promise.all([
                backendClient.getAssets(),
                backendClient.getPortfolio(),
            ]);

            setAssets(loadedAssets);
            setPortfolio(loadedPortfolio);

            if (loadedAssets.length > 0 && !formState.ticker) {
                setFormState((current) => ({
                    ...current,
                    ticker: loadedAssets[0].ticker,
                }));
            }
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось загрузить портфель");
        } finally {
            setIsLoading(false);
        }
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const ticker = formState.ticker.trim().toUpperCase();
        const quantity = Number(formState.quantity);
        const averageBuyPrice = Number(formState.averageBuyPrice);

        if (!ticker) {
            setError("Выбери тикер актива");
            return;
        }

        if (!Number.isFinite(quantity) || quantity <= 0) {
            setError("Количество должно быть больше 0");
            return;
        }

        if (!Number.isFinite(averageBuyPrice) || averageBuyPrice <= 0) {
            setError("Средняя цена покупки должна быть больше 0");
            return;
        }

        try {
            setIsSavingPosition(true);
            setError("");

            await backendClient.addPortfolioPosition({
                ticker,
                quantity,
                averageBuyPrice,
            });

            setFormState((current) => ({
                ...initialFormState,
                ticker: current.ticker,
            }));
            setAiReport(null);

            const updatedPortfolio = await backendClient.getPortfolio();
            setPortfolio(updatedPortfolio);
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось добавить позицию");
        } finally {
            setIsSavingPosition(false);
        }
    }

    async function handleDelete(ticker: string) {
        try {
            setDeletingTicker(ticker);
            setError("");

            await backendClient.removePortfolioPosition(ticker);
            setAiReport(null);

            const updatedPortfolio = await backendClient.getPortfolio();
            setPortfolio(updatedPortfolio);
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось удалить позицию");
        } finally {
            setDeletingTicker(null);
        }
    }

    async function handleGenerateAIReport() {
        try {
            setIsGeneratingReport(true);
            setError("");

            const report = await backendClient.generateAIPortfolioReport();
            setAiReport(report);
        } catch (error: unknown) {
            setError(
                error instanceof Error
                    ? error.message
                    : "Не удалось создать AI-отчёт по портфелю"
            );
        } finally {
            setIsGeneratingReport(false);
        }
    }

    if (isLoading) {
        return <LoadingBlock text="Загружаем портфель..." />;
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Портфель</p>
                    <h1>Инвестиционный портфель</h1>
                </div>

                <div className="hero-actions">
                    <button
                        type="button"
                        className="primary-button"
                        disabled={!hasPositions || isGeneratingReport}
                        onClick={handleGenerateAIReport}
                    >
                        {isGeneratingReport ? "Генерируем..." : "AI-анализ портфеля"}
                    </button>
                </div>
            </div>

            {error && <div className="error-block">{error}</div>}

            <div className="stats-grid">
                <article className="stat-card">
                    <span>Вложено</span>
                    <strong>{formatMoney(portfolio?.totalInvested)}</strong>
                </article>

                <article className="stat-card">
                    <span>Текущая стоимость</span>
                    <strong>{formatMoney(portfolio?.totalCurrentValue)}</strong>
                </article>

                <article className="stat-card">
                    <span>Прибыль / убыток</span>
                    <strong className={getProfitClass(portfolio?.totalProfitLoss)}>
                        {formatMoney(portfolio?.totalProfitLoss)}
                    </strong>
                    <small>{formatPercent(portfolio?.totalProfitLossPercent)}</small>
                </article>

                <article className="stat-card">
                    <span>Позиций</span>
                    <strong>{portfolio?.positionsCount ?? 0}</strong>
                    <small>
                        Обновлено: {portfolio?.calculatedAt ? formatDate(portfolio.calculatedAt) : "—"}
                    </small>
                </article>
            </div>

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Добавить позицию</h2>
                    </div>
                </div>

                <form className="portfolio-form" onSubmit={handleSubmit}>
                    <label>
                        <span>Поиск актива</span>
                        <input
                            type="text"
                            value={assetSearch}
                            placeholder="SBER, BTCUSDT, Яндекс..."
                            onChange={(event) => setAssetSearch(event.target.value)}
                        />
                    </label>

                    <label>
                        <span>Тикер</span>
                        <select
                            value={formState.ticker}
                            onChange={(event) =>
                                setFormState((current) => ({
                                    ...current,
                                    ticker: event.target.value,
                                }))
                            }
                        >
                            {filteredAssets.map((asset) => (
                                <option value={asset.ticker} key={asset.id}>
                                    {asset.ticker} — {asset.name}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label>
                        <span>Количество</span>
                        <input
                            type="number"
                            step="any"
                            min="0"
                            value={formState.quantity}
                            placeholder="10"
                            onChange={(event) =>
                                setFormState((current) => ({
                                    ...current,
                                    quantity: event.target.value,
                                }))
                            }
                        />
                    </label>

                    <label>
                        <span>Средняя цена покупки</span>
                        <input
                            type="number"
                            step="any"
                            min="0"
                            value={formState.averageBuyPrice}
                            placeholder="250"
                            onChange={(event) =>
                                setFormState((current) => ({
                                    ...current,
                                    averageBuyPrice: event.target.value,
                                }))
                            }
                        />
                    </label>

                    <button
                        type="submit"
                        className="primary-button"
                        disabled={isSavingPosition || filteredAssets.length === 0}
                    >
                        {isSavingPosition ? "Сохраняем..." : "Добавить позицию"}
                    </button>
                </form>
            </article>

            {aiReport && (
                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <p className="eyebrow">AI-отчёт по портфелю</p>
                            <h2>AI-анализ портфеля</h2>
                            <p>
                                Провайдер: <strong>{aiReport.provider}</strong> · Риск:{" "}
                                <span className={`risk risk-${aiReport.riskLevel.toLowerCase()}`}>
                                    {aiReport.riskLevel}
                                </span>
                            </p>
                        </div>
                    </div>

                    {aiReport.fallbackReason && (
                        <div className="error-block">
                            <strong>Причина fallback:</strong>
                            <pre>{aiReport.fallbackReason}</pre>
                        </div>
                    )}

                    <div className="ai-report-grid">
                        <div>
                            <h3>Краткий вывод</h3>
                            <p>{aiReport.summary}</p>
                        </div>

                        <div>
                            <h3>Риск-скор</h3>
                            <strong>{aiReport.riskScore} / 100</strong>
                            <p>Уверенность: {formatPercent(aiReport.confidence * 100)}</p>
                        </div>
                    </div>

                    <div className="ai-factors-grid">
                        <div>
                            <h3>Позитивные факторы</h3>
                            <ul>
                                {aiReport.positiveFactors.map((factor) => (
                                    <li key={factor}>{factor}</li>
                                ))}
                            </ul>
                        </div>

                        <div>
                            <h3>Негативные факторы</h3>
                            <ul>
                                {aiReport.negativeFactors.map((factor) => (
                                    <li key={factor}>{factor}</li>
                                ))}
                            </ul>
                        </div>
                    </div>

                    <div className="ai-explanation">
                        <h3>Объяснение</h3>
                        <p>{aiReport.explanation}</p>
                        <small>{aiReport.disclaimer}</small>
                    </div>
                </article>
            )}

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Позиции</h2>
                    </div>
                </div>

                {!hasPositions ? (
                    <div className="empty-state">
                        <h3>Портфель пока пуст</h3>
                    </div>
                ) : (
                    <div className="table-wrapper">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Актив</th>
                                <th>Тип</th>
                                <th>Количество</th>
                                <th>Средняя</th>
                                <th>Текущая</th>
                                <th>Стоимость</th>
                                <th>Прибыль/убыток</th>
                                <th>Источник</th>
                                <th></th>
                            </tr>
                            </thead>

                            <tbody>
                            {sortedPositions.map((position) => (
                                <tr key={position.id}>
                                    <td>
                                        <Link to={`/assets/${position.ticker}`}>
                                            <strong>{position.ticker}</strong>
                                        </Link>
                                        <small>{position.name}</small>
                                    </td>

                                    <td>{position.assetType}</td>
                                    <td>{formatNumber(position.quantity)}</td>
                                    <td>{formatMoney(position.averageBuyPrice, position.currency)}</td>
                                    <td>{formatMoney(position.currentPrice, position.currency)}</td>
                                    <td>{formatMoney(position.currentValue, position.currency)}</td>
                                    <td>
                                        <strong className={getProfitClass(position.profitLoss)}>
                                            {formatMoney(position.profitLoss, position.currency)}
                                        </strong>
                                        <small>{formatPercent(position.profitLossPercent)}</small>
                                    </td>
                                    <td>
                                        <span>{position.priceSource}</span>
                                        <small>{formatDate(position.priceTimestamp)}</small>
                                    </td>
                                    <td>
                                        <button
                                            type="button"
                                            className="ghost-button"
                                            disabled={deletingTicker === position.ticker}
                                            onClick={() => handleDelete(position.ticker)}
                                        >
                                            {deletingTicker === position.ticker
                                                ? "Удаляем..."
                                                : "Удалить"}
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </article>
        </section>
    );
}

function formatNumber(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "—";
    }

    return new Intl.NumberFormat("ru-RU", {
        maximumFractionDigits: 8,
    }).format(value);
}

function formatMoney(
    value: number | null | undefined,
    currency = "RUB"
): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "—";
    }

    return new Intl.NumberFormat("ru-RU", {
        maximumFractionDigits: 6,
    }).format(value) + ` ${currency}`;
}

function formatPercent(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "—";
    }

    return `${new Intl.NumberFormat("ru-RU", {
        maximumFractionDigits: 2,
    }).format(value)}%`;
}

function formatDate(value: string | null | undefined): string {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime()) || date.getFullYear() <= 1971) {
        return "—";
    }

    return new Intl.DateTimeFormat("ru-RU", {
        dateStyle: "short",
        timeStyle: "short",
    }).format(date);
}

function getProfitClass(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "";
    }

    if (value > 0) {
        return "positive-value";
    }

    if (value < 0) {
        return "negative-value";
    }

    return "";
}