package com.investnavigator.backend.marketdata.repository;

import com.investnavigator.backend.asset.model.Asset;
import com.investnavigator.backend.marketdata.model.Candle;
import com.investnavigator.backend.marketdata.model.Timeframe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandleRepository extends JpaRepository<Candle, UUID> {

    List<Candle> findByAssetAndTimeframeOrderByTimestampAsc(Asset asset, Timeframe timeframe);

    List<Candle> findTop30ByAssetAndTimeframeOrderByTimestampDesc(Asset asset, Timeframe timeframe);
}