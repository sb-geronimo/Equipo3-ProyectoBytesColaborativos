package es.bytescolab.msdashboard.service;

import es.bytescolab.msdashboard.dto.response.RankingResponse;
import es.bytescolab.msdashboard.enums.RankingMetric;

public interface RankingService {

    RankingResponse getRanking(RankingMetric metric, int days, int limit, String type);
}
