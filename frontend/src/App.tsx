import { Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/AppLayout";
import { AssetDetailsPage } from "./pages/AssetDetailsPage";
import { AssetsPage } from "./pages/AssetsPage";
import { ComparePage } from "./pages/ComparePage";
import { DashboardPage } from "./pages/DashboardPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { WatchlistPage } from "./pages/WatchlistPage";

export function App() {
    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route index element={<DashboardPage />} />
                <Route path="assets" element={<AssetsPage />} />
                <Route path="assets/:ticker" element={<AssetDetailsPage />} />
                <Route path="compare" element={<ComparePage />} />
                <Route path="watchlist" element={<WatchlistPage />} />
                <Route path="*" element={<NotFoundPage />} />
            </Route>
        </Routes>
    );
}