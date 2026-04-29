import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { backendClient } from "../api/backendClient";
import { LoadingBlock } from "../components/LoadingBlock";
import type {
    AIReportResponse,
    AnalyticsSummaryResponse,
    AssetResponse,
    CandleResponse,
    MarketPriceResponse,
} from "../types/api";

export function AssetDetailsPage() {
    const { ticker } = useParams<{ ticker: string }>();

    const [asset, setAsset] = useState<AssetResponse | null>(null);
    const [marketData, setMarketData] = useState<MarketPriceResponse | null>(null);
    const [candles, setCandles] = useState<CandleResponse[]>([]);
    const [analytics, setAnalytics] = useState<AnalyticsSummaryResponse | null>(null);
    const [reports, setReports] = useState<AIReportResponse[]>([]);

    const [isLoading, setIsLoading] = useState(true);
    const [isGeneratingReport, setIsGeneratingReport] = useState(false);
    const [error, setError] = useState("");
    const [warning, setWarning] = useState("");

    useEffect(() => {
        if (!ticker) {
            return;
        }

        setIsLoading(true);
        setError("");
        setWarning("");

        async function loadAssetDetails() {
            try {
                const loadedAsset = await backendClient.getAsset(ticker!);
                setAsset(loadedAsset);

                const results = await Promise.allSettled([
                    backendClient.getMarketData(ticker!),
                    backendClient.getCandles(ticker!),
                    backendClient.getAnalyticsSummary(ticker!),
                    backendClient.getAIReports(ticker!),
                ]);

                const [marketDataResult, candlesResult, analyticsResult, reportsResult] = results;

                if (marketDataResult.status === "fulfilled") {
                    setMarketData(marketDataResult.value);
                }

                if (candlesResult.status === "fulfilled") {
                    setCandles(candlesResult.value);
                }

                if (analyticsResult.status === "fulfilled") {
                    setAnalytics(analyticsResult.value);
                } else {
                    setWarning("Для этого актива пока нет полной аналитики. Возможно, не хватает свечей.");
                }

                if (reportsResult.status === "fulfilled") {
                    setReports(reportsResult.value);
                }
            } catch (error: unknown) {
                setError(error instanceof Error ? error.message : "Не удалось загрузить карточку актива");
            } finally {
                setIsLoading(false);
            }
        }

        loadAssetDetails();
    }, [ticker]);

    async function handleGenerateReport() {
        if (!ticker) {
            return;
        }

        setIsGeneratingReport(true);
        setError("");

        try {
            const newReport = await backendClient.generateAIReport(ticker);
            setReports((previousReports) => [newReport, ...previousReports]);
            setWarning("");
        } catch (error: unknown) {
            setError(error instanceof Error ? error.message : "Не удалось создать AI-отчёт");
        } finally {
            setIsGeneratingReport(false);
        }
    }

    if (isLoading) {
        return <LoadingBlock text="Загружаем карточку актива..." />;
    }

    if (error && !asset) {
        return (
            <section className="page">
                <div className="error-block">{error}</div>
                <Link to="/assets" className="secondary-link">
                    Вернуться к активам
                </Link>
            </section>
        );
    }

    return (
        <section className="page">
            <Link to="/assets" className="secondary-link">
                ← Назад к активам
            </Link>

            {error && <div className="error-block">{error}</div>}
            {warning && <div className="warning-block">{warning}</div>}

            {asset && (
                <div className="asset-details-header">
                    <div>
                        <p className="eyebrow">{asset.exchange}</p>
                        <h1>{asset.ticker}</h1>
                        <p>{asset.name}</p>
                    </div>

                    <div className="badge-row">
                        <span>{asset.assetType}</span>
                        <span>{asset.currency}</span>
                        <span>{asset.active ? "Active" : "Inactive"}</span>
                    </div>
                </div>
            )}

            <div className="grid-3">
                <article className="info-card">
                    <h3>Текущая цена</h3>
                    <strong className="big-number">{marketData?.price ?? "—"}</strong>
                    <p>Источник: {marketData?.source ?? "—"}</p>
                </article>

                <article className="info-card">
                    <h3>Изменение</h3>
                    <strong className="big-number">
                        {analytics ? `${analytics.priceChangePercent}%` : "—"}
                    </strong>
                    <p>По доступным дневным свечам</p>
                </article>

                <article className="info-card">
                    <h3>Риск</h3>
                    <strong className={`risk risk-${analytics?.riskLevel?.toLowerCase() ?? "unknown"}`}>
                        {analytics?.riskLevel ?? "—"}
                    </strong>
                    <p>Score: {analytics?.riskScore ?? "—"} / 100</p>
                </article>
            </div>

            <div className="content-grid">
                <article className="panel">
                    <h2>Аналитика</h2>

                    {analytics ? (
                        <div className="metrics-table">
                            <div>
                                <span>Текущая цена</span>
                                <strong>{analytics.currentPrice}</strong>
                            </div>
                            <div>
                                <span>Первая цена закрытия</span>
                                <strong>{analytics.firstClose}</strong>
                            </div>
                            <div>
                                <span>Последняя цена закрытия</span>
                                <strong>{analytics.lastClose}</strong>
                            </div>
                            <div>
                                <span>Средний объём</span>
                                <strong>{analytics.averageVolume}</strong>
                            </div>
                            <div>
                                <span>Волатильность</span>
                                <strong>{analytics.volatilityPercent}%</strong>
                            </div>
                            <div>
                                <span>Точек данных</span>
                                <strong>{analytics.dataPoints}</strong>
                            </div>
                        </div>
                    ) : (
                        <p>Аналитика пока недоступна для этого актива.</p>
                    )}
                </article>

                <article className="panel">
                    <div className="panel-header">
                        <div>
                            <h2>AI-отчёты</h2>
                            <p>Mock-отчёты на основе рассчитанных метрик.</p>
                        </div>

                        <button
                            type="button"
                            className="primary-button"
                            disabled={isGeneratingReport}
                            onClick={handleGenerateReport}
                        >
                            {isGeneratingReport ? "Формируем..." : "Создать отчёт"}
                        </button>
                    </div>

                    <div className="report-list">
                        {reports.length === 0 && <p>Пока нет AI-отчётов.</p>}

                        {reports.map((report) => (
                            <article className="report-card" key={report.id}>
                                <div className="report-card-header">
                                    <strong>{report.riskLevel}</strong>
                                    <span>{new Date(report.createdAt).toLocaleString("ru-RU")}</span>
                                </div>

                                <p>{report.summary}</p>

                                <h4>Позитивные факторы</h4>
                                <ul>
                                    {report.positiveFactors.map((factor) => (
                                        <li key={factor}>{factor}</li>
                                    ))}
                                </ul>

                                <h4>Негативные факторы</h4>
                                <ul>
                                    {report.negativeFactors.map((factor) => (
                                        <li key={factor}>{factor}</li>
                                    ))}
                                </ul>

                                <details>
                                    <summary>Объяснение</summary>
                                    <p>{report.explanation}</p>
                                    <small>{report.disclaimer}</small>
                                </details>
                            </article>
                        ))}
                    </div>
                </article>
            </div>

            <article className="panel">
                <h2>Свечи</h2>

                {candles.length === 0 ? (
                    <p>Свечи пока недоступны.</p>
                ) : (
                    <div className="candles-table">
                        <div className="candles-row candles-head">
                            <span>Дата</span>
                            <span>Open</span>
                            <span>High</span>
                            <span>Low</span>
                            <span>Close</span>
                            <span>Volume</span>
                        </div>

                        {candles.map((candle) => (
                            <div className="candles-row" key={candle.timestamp}>
                                <span>{new Date(candle.timestamp).toLocaleDateString("ru-RU")}</span>
                                <span>{candle.open}</span>
                                <span>{candle.high}</span>
                                <span>{candle.low}</span>
                                <span>{candle.close}</span>
                                <span>{candle.volume}</span>
                            </div>
                        ))}
                    </div>
                )}
            </article>
        </section>
    );
}