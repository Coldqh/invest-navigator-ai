import { useEffect, useMemo, useState } from "react";
import { backendClient } from "../api/backendClient";
import { LoadingBlock } from "../components/LoadingBlock";
import type {
    AICompareReportResponse,
    AnalyticsSummaryResponse,
    AssetResponse,
} from "../types/api";

export function ComparePage() {
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [selectedTickers, setSelectedTickers] = useState<string[]>(["SBER", "GAZP"]);
    const [results, setResults] = useState<AnalyticsSummaryResponse[]>([]);
    const [aiReport, setAiReport] = useState<AICompareReportResponse | null>(null);
    const [isLoadingAssets, setIsLoadingAssets] = useState(true);
    const [isComparing, setIsComparing] = useState(false);
    const [isGeneratingAIReport, setIsGeneratingAIReport] = useState(false);
    const [error, setError] = useState("");

    const selectedAssets = useMemo(() => {
        return assets.filter((asset) => selectedTickers.includes(asset.ticker));
    }, [assets, selectedTickers]);

    useEffect(() => {
        backendClient
            .getAssets()
            .then(setAssets)
            .catch((error: unknown) => {
                setError(error instanceof Error ? error.message : "Не удалось загрузить активы");
            })
            .finally(() => setIsLoadingAssets(false));
    }, []);

    useEffect(() => {
        if (selectedTickers.length >= 2) {
            handleCompare();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function toggleTicker(ticker: string) {
        setSelectedTickers((previousTickers) => {
            setAiReport(null);

            if (previousTickers.includes(ticker)) {
                return previousTickers.filter((item) => item !== ticker);
            }

            if (previousTickers.length >= 5) {
                setError("Можно сравнивать не больше 5 активов одновременно");
                return previousTickers;
            }

            setError("");
            return [...previousTickers, ticker];
        });
    }

    async function handleCompare() {
        setError("");
        setAiReport(null);

        if (selectedTickers.length < 2) {
            setError("Выбери минимум 2 актива для сравнения");
            return;
        }

        setIsComparing(true);

        try {
            const comparison = await backendClient.compareAssets(selectedTickers);
            setResults(comparison);
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось сравнить активы");
        } finally {
            setIsComparing(false);
        }
    }

    async function handleGenerateAIReport() {
        setError("");

        if (selectedTickers.length !== 2) {
            setError("AI-анализ сравнения доступен для двух активов");
            return;
        }

        setIsGeneratingAIReport(true);

        try {
            const report = await backendClient.generateAICompareReport(
                selectedTickers[0],
                selectedTickers[1]
            );

            setAiReport(report);
        } catch (error: unknown) {
            setError(
                error instanceof Error
                    ? error.message
                    : "Не удалось создать AI-анализ сравнения"
            );
        } finally {
            setIsGeneratingAIReport(false);
        }
    }

    if (isLoadingAssets) {
        return <LoadingBlock text="Загружаем активы для сравнения..." />;
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Сравнение</p>
                    <h1>Сравнение активов</h1>
                </div>
            </div>

            {error && <div className="error-block">{error}</div>}

            <article className="panel">
                <div className="panel-header">
                    <div>
                        <h2>Выбор активов</h2>
                    </div>

                    <div className="hero-actions">
                        <button
                            type="button"
                            className="primary-button"
                            disabled={isComparing || selectedTickers.length < 2}
                            onClick={handleCompare}
                        >
                            {isComparing ? "Сравниваем..." : "Сравнить"}
                        </button>

                        <button
                            type="button"
                            className="ghost-button"
                            disabled={isGeneratingAIReport || selectedTickers.length !== 2}
                            onClick={handleGenerateAIReport}
                        >
                            {isGeneratingAIReport ? "Генерируем..." : "AI-анализ"}
                        </button>
                    </div>
                </div>

                <div className="compare-picker">
                    {assets.map((asset) => {
                        const isSelected = selectedTickers.includes(asset.ticker);

                        return (
                            <button
                                type="button"
                                key={asset.id}
                                className={isSelected ? "compare-chip compare-chip-active" : "compare-chip"}
                                onClick={() => toggleTicker(asset.ticker)}
                            >
                                <strong>{asset.ticker}</strong>
                                <span>{asset.exchange}</span>
                            </button>
                        );
                    })}
                </div>

                {selectedAssets.length > 0 && (
                    <p className="compare-selection">
                        Выбрано: {selectedAssets.map((asset) => asset.ticker).join(", ")}
                    </p>
                )}
            </article>

            {aiReport && (
                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>AI-анализ сравнения</h2>
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
                        {aiReport.assetFactors.map((assetFactors) => (
                            <div key={assetFactors.ticker}>
                                <h3>{assetFactors.ticker}</h3>

                                <h4>Плюсы</h4>
                                <ul>
                                    {assetFactors.positiveFactors.map((factor) => (
                                        <li key={factor}>{factor}</li>
                                    ))}
                                </ul>

                                <h4>Минусы</h4>
                                <ul>
                                    {assetFactors.negativeFactors.map((factor) => (
                                        <li key={factor}>{factor}</li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>

                    <div className="ai-explanation">
                        <h3>Объяснение</h3>
                        <p>{aiReport.explanation}</p>
                        <small>{aiReport.disclaimer}</small>
                    </div>
                </article>
            )}

            <article className="panel">
                <h2>Результаты сравнения</h2>

                {results.length === 0 ? (
                    <div className="empty-state">
                        <h3>Нет результатов</h3>
                    </div>
                ) : (
                    <div className="compare-table-wrapper">
                        <table className="compare-table">
                            <thead>
                            <tr>
                                <th>Метрика</th>
                                {results.map((asset) => (
                                    <th key={asset.ticker}>{asset.ticker}</th>
                                ))}
                            </tr>
                            </thead>

                            <tbody>
                            <tr>
                                <td>Название</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>{asset.name}</td>
                                ))}
                            </tr>

                            <tr>
                                <td>Текущая цена</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>{asset.currentPrice}</td>
                                ))}
                            </tr>

                            <tr>
                                <td>Изменение, %</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>{asset.priceChangePercent}%</td>
                                ))}
                            </tr>

                            <tr>
                                <td>Волатильность, %</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>{asset.volatilityPercent}%</td>
                                ))}
                            </tr>

                            <tr>
                                <td>Средний объём</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>{asset.averageVolume}</td>
                                ))}
                            </tr>

                            <tr>
                                <td>Риск-скор</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>{asset.riskScore} / 100</td>
                                ))}
                            </tr>

                            <tr>
                                <td>Уровень риска</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>
                                        <span className={`risk risk-${asset.riskLevel.toLowerCase()}`}>
                                            {asset.riskLevel}
                                        </span>
                                    </td>
                                ))}
                            </tr>

                            <tr>
                                <td>Точек данных</td>
                                {results.map((asset) => (
                                    <td key={asset.ticker}>{asset.dataPoints}</td>
                                ))}
                            </tr>
                            </tbody>
                        </table>
                    </div>
                )}
            </article>
        </section>
    );
}

function formatPercent(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "—";
    }

    return `${new Intl.NumberFormat("ru-RU", {
        maximumFractionDigits: 2,
    }).format(value)}%`;
}