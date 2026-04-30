import { useEffect, useMemo, useState } from "react";
import { backendClient } from "../api/backendClient";
import { LoadingBlock } from "../components/LoadingBlock";
import type { AnalyticsSummaryResponse, AssetResponse } from "../types/api";

export function ComparePage() {
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [selectedTickers, setSelectedTickers] = useState<string[]>(["SBER", "GAZP"]);
    const [results, setResults] = useState<AnalyticsSummaryResponse[]>([]);
    const [isLoadingAssets, setIsLoadingAssets] = useState(true);
    const [isComparing, setIsComparing] = useState(false);
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

                    <button
                        type="button"
                        className="primary-button"
                        disabled={isComparing || selectedTickers.length < 2}
                        onClick={handleCompare}
                    >
                        {isComparing ? "Сравниваем..." : "Сравнить"}
                    </button>
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