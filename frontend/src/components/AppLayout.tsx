import { NavLink, Outlet } from "react-router-dom";

export function AppLayout() {
    return (
        <div className="app-shell">
            <header className="app-header">
                <NavLink to="/" className="brand">
                    <span className="brand-mark">IN</span>
                    <div>
                        <strong>Invest Navigator AI</strong>
                        <small>Market analytics platform</small>
                    </div>
                </NavLink>

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
                        Watchlist
                    </NavLink>
                </nav>
            </header>

            <main className="app-main">
                <Outlet />
            </main>
        </div>
    );
}