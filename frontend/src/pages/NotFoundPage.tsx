import { Link } from "react-router-dom";

export function NotFoundPage() {
    return (
        <section className="page">
            <div className="hero-card">
                <p className="eyebrow">404</p>
                <h1>Страница не найдена</h1>
                <p>Такого раздела пока нет.</p>

                <Link to="/" className="primary-button">
                    На дашборд
                </Link>
            </div>
        </section>
    );
}