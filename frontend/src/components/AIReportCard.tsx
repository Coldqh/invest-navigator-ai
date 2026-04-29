import type { AIReportResponse } from "../types/api";

type AIReportCardProps = {
    report: AIReportResponse;
};

export function AIReportCard({ report }: AIReportCardProps) {
    const createdAt = new Date(report.createdAt).toLocaleString("ru-RU");

    return (
        <article className="ai-report-card">
            <div className="ai-report-top">
                <div>
                    <p className="eyebrow">AI report</p>
                    <h3>{report.ticker}</h3>
                    <span>{createdAt}</span>
                </div>

                <div className="ai-report-score">
          <span className={`risk risk-${report.riskLevel.toLowerCase()}`}>
            {report.riskLevel}
          </span>
                    <strong>{report.riskScore}/100</strong>
                    <small>risk score</small>
                </div>
            </div>

            <div className="ai-report-meta">
                <div>
                    <span>AI-провайдер</span>
                    <strong>{report.provider}</strong>
                </div>

                <div>
                    <span>Уверенность</span>
                    <strong>{report.confidence}</strong>
                </div>

                <div>
                    <span>Актив</span>
                    <strong>{report.name}</strong>
                </div>
            </div>

            <div className="ai-report-summary">
                <h4>Краткий вывод</h4>
                <p>{report.summary}</p>
            </div>

            <div className="ai-report-grid">
                <section className="factor-panel factor-positive">
                    <h4>Позитивные факторы</h4>

                    <ul>
                        {report.positiveFactors.map((factor) => (
                            <li key={factor}>{factor}</li>
                        ))}
                    </ul>
                </section>

                <section className="factor-panel factor-negative">
                    <h4>Негативные факторы</h4>

                    <ul>
                        {report.negativeFactors.map((factor) => (
                            <li key={factor}>{factor}</li>
                        ))}
                    </ul>
                </section>
            </div>

            <details className="ai-report-details">
                <summary>Показать объяснение модели</summary>

                <div className="ai-report-explanation">
                    <p>{report.explanation}</p>
                    <small>{report.disclaimer}</small>
                </div>
            </details>
        </article>
    );
}