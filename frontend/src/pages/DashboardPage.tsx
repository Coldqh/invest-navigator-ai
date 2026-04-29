import { Link } from "react-router-dom";

export function DashboardPage() {
    return (
        <section className="page">
            <div className="hero-card">
                <p className="eyebrow">MVP platform</p>
                <h1>ИнвестНавигатор ИИ</h1>
                <p>
                    Платформа инвестиционного анализа: активы, цены, метрики, риск и AI-отчёты.
                </p>

                <div className="hero-actions">
                    <Link to="/assets" className="primary-button">
                        Открыть активы
                    </Link>
                </div>
            </div>
        </section>
    );
}