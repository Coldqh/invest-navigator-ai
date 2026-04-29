import { useMemo } from "react";
import type { CandleResponse } from "../types/api";

type MiniLineChartProps = {
    candles: CandleResponse[];
};

export function MiniLineChart({ candles }: MiniLineChartProps) {
    const chartData = useMemo(() => {
        const width = 720;
        const height = 220;
        const padding = 24;

        if (candles.length === 0) {
            return {
                width,
                height,
                points: "",
                minClose: 0,
                maxClose: 0,
                firstClose: 0,
                lastClose: 0,
                isPositive: false,
            };
        }

        const closes = candles.map((candle) => Number(candle.close));
        const minClose = Math.min(...closes);
        const maxClose = Math.max(...closes);
        const firstClose = closes[0];
        const lastClose = closes[closes.length - 1];

        const priceRange = maxClose - minClose || 1;
        const stepX = candles.length > 1
            ? (width - padding * 2) / (candles.length - 1)
            : 0;

        const points = closes
            .map((close, index) => {
                const x = padding + index * stepX;
                const normalized = (close - minClose) / priceRange;
                const y = height - padding - normalized * (height - padding * 2);

                return `${x.toFixed(2)},${y.toFixed(2)}`;
            })
            .join(" ");

        return {
            width,
            height,
            points,
            minClose,
            maxClose,
            firstClose,
            lastClose,
            isPositive: lastClose >= firstClose,
        };
    }, [candles]);

    if (candles.length === 0) {
        return (
            <div className="mini-chart-empty">
                Недостаточно свечей для построения графика.
            </div>
        );
    }

    return (
        <div className="mini-chart-card">
            <div className="mini-chart-header">
                <div>
                    <h3>Динамика close</h3>
                    <p>Мини-график по доступным дневным свечам</p>
                </div>

                <div className={chartData.isPositive ? "chart-change positive" : "chart-change negative"}>
                    {chartData.isPositive ? "Рост" : "Снижение"}
                </div>
            </div>

            <svg
                className="mini-chart"
                viewBox={`0 0 ${chartData.width} ${chartData.height}`}
                role="img"
                aria-label="График цены закрытия"
            >
                <line x1="24" y1="24" x2="24" y2="196" className="chart-axis" />
                <line x1="24" y1="196" x2="696" y2="196" className="chart-axis" />

                <polyline
                    points={chartData.points}
                    className={chartData.isPositive ? "chart-line chart-line-positive" : "chart-line chart-line-negative"}
                />

                {chartData.points.split(" ").map((point) => {
                    const [x, y] = point.split(",");

                    return (
                        <circle
                            key={point}
                            cx={x}
                            cy={y}
                            r="4"
                            className="chart-point"
                        />
                    );
                })}
            </svg>

            <div className="mini-chart-footer">
                <span>Min close: {chartData.minClose}</span>
                <span>Max close: {chartData.maxClose}</span>
                <span>Last close: {chartData.lastClose}</span>
            </div>
        </div>
    );
}