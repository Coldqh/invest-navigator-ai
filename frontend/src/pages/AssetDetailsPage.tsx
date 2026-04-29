import { Link, useParams } from "react-router-dom";

export function AssetDetailsPage() {
    const { ticker } = useParams<{ ticker: string }>();

    return (
        <section className="page">
            <Link to="/assets" className="secondary-link">
                ← Назад к активам
            </Link>

            <div className="asset-details-header">
                <div>
                    <p className="eyebrow">Asset details</p>
                    <h1>{ticker}</h1>
                    <p>Карточка актива. Следующим шагом подключим реальные данные из backend.</p>
                </div>
            </div>
        </section>
    );
}