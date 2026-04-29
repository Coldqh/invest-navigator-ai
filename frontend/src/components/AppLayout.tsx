import { Link, NavLink, Outlet } from "react-router-dom";

export function AppLayout() {
    return (
        <div className="app-shell">
            <aside className="sidebar">
                <Link to="/" className="brand">
                    <span className="brand-mark">ИИ</span>
                    <span>
            <strong>ИнвестНавигатор</strong>
            <small>Investment analysis platform</small>
          </span>
                </Link>

                <nav className="nav">
                    <NavLink to="/" end>
                        Дашборд
                    </NavLink>
                    <NavLink to="/assets">
                        Активы
                    </NavLink>
                </nav>
            </aside>

            <main className="main">
                <Outlet />
            </main>
        </div>
    );
}