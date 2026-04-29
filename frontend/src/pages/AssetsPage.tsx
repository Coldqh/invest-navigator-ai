import { Link } from "react-router-dom";

const demoAssets = ["SBER", "GAZP", "LKOH", "YNDX", "BTCUSDT", "ETHUSDT"];

export function AssetsPage() {
    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Assets</p>
                    <h1>Активы</h1>
                    <p>Пока тестовый список. Следующим шагом подключим backend.</p>
                </div>
            </div>

            <div className="asset-list">
                {demoAssets.map((ticker) => (
                    <Link to={`/assets/${ticker}`} className="asset-card" key={ticker}>
                        <div>
                            <h3>{ticker}</h3>
                            <p>Демо-актив</p>
                        </div>
                    </Link>
                ))}
            </div>
        </section>
    );
}