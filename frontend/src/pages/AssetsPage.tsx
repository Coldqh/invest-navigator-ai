import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { backendClient } from "../api/backendClient";
import { LoadingBlock } from "../components/LoadingBlock";
import type { AssetResponse } from "../types/api";

export function AssetsPage() {
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [query, setQuery] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");

    const filteredAssets = useMemo(() => {
        const normalizedQuery = query.trim().toLowerCase();

        if (!normalizedQuery) {
            return assets;
        }

        return assets.filter((asset) => {
            return (
                asset.ticker.toLowerCase().includes(normalizedQuery) ||
                asset.name.toLowerCase().includes(normalizedQuery)
            );
        });
    }, [assets, query]);

    useEffect(() => {
        backendClient
            .getAssets()
            .then(setAssets)
            .catch((error: unknown) => {
                setError(error instanceof Error ? error.message : "Не удалось загрузить активы");
            })
            .finally(() => setIsLoading(false));
    }, []);

    if (isLoading) {
        return <LoadingBlock />;
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Активы</p>
                    <h1>Активы</h1>
                </div>
            </div>

            {error && <div className="error-block">{error}</div>}

            <div className="toolbar">
                <input
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Поиск по тикеру или названию..."
                />
            </div>

            <div className="asset-list">
                {filteredAssets.map((asset) => (
                    <Link to={`/assets/${asset.ticker}`} className="asset-card" key={asset.id}>
                        <div>
                            <h3>{asset.ticker}</h3>
                            <p>{asset.name}</p>
                        </div>

                        <div className="asset-meta">
                            <span>{asset.assetType}</span>
                            <span>{asset.exchange}</span>
                            <span>{asset.currency}</span>
                        </div>
                    </Link>
                ))}
            </div>
        </section>
    );
}