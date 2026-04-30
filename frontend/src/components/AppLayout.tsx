import { NavLink, Outlet } from "react-router-dom";

export function AppLayout() {
    return (
        <div className="app-shell">
            <header className="app-header">
                <nav className="nav">
                    <NavLink to="/" end>
                        Дашборд
                    </NavLink>
                    <NavLink to="/assets">
                        Активы
                    </NavLink>
                    <NavLink to="/compare">
                        Сравнение
                    </NavLink>
                    <NavLink to="/watchlist">
                        Избранное
                    </NavLink>
                    <NavLink to="/portfolio">
                        Портфель
                    </NavLink>
                </nav>
            </header>

            <main className="app-main">
                <Outlet />
            </main>
        </div>
    );
}