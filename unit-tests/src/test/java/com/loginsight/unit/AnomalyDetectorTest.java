package com.loginsight.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies the anomaly detection logic that compares a service's current log
 * emission rate against its rolling baseline.
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectorTest {

    @Mock
    private LogMetricsRepository repository;

    @Test
    @DisplayName("A sharp rate spike over the baseline is flagged as an anomaly")
    void whenSpikeDetected_thenAnomalyFlagged() {
        when(repository.getDataPointCount("api")).thenReturn(50L);
        when(repository.getBaselineRate("api")).thenReturn(100.0);
        when(repository.getCurrentRate("api")).thenReturn(450.0);

        AnomalyDetector detector = new AnomalyDetector(repository, 3.0, 10);

        assertThat(detector.evaluate("api")).isEqualTo(AnomalyStatus.ANOMALY);
    }

    @Test
    @DisplayName("A current rate close to the baseline is considered normal")
    void whenBaseline_thenNoAnomaly() {
        when(repository.getDataPointCount("api")).thenReturn(50L);
        when(repository.getBaselineRate("api")).thenReturn(100.0);
        when(repository.getCurrentRate("api")).thenReturn(110.0);

        AnomalyDetector detector = new AnomalyDetector(repository, 3.0, 10);

        assertThat(detector.evaluate("api")).isEqualTo(AnomalyStatus.NORMAL);
    }

    @Test
    @DisplayName("Too few data points suppresses detection to avoid cold-start noise")
    void whenColdStart_thenSuppressed() {
        when(repository.getDataPointCount("api")).thenReturn(3L);
        lenient().when(repository.getBaselineRate("api")).thenReturn(100.0);
        lenient().when(repository.getCurrentRate("api")).thenReturn(450.0);

        AnomalyDetector detector = new AnomalyDetector(repository, 3.0, 10);

        assertThat(detector.evaluate("api")).isEqualTo(AnomalyStatus.SUPPRESSED);
    }

    /**
     * Source of the per-service rate metrics the detector reasons over. In the
     * full platform this is backed by a time-series store; here it is mocked.
     */
    interface LogMetricsRepository {
        double getBaselineRate(String service);

        double getCurrentRate(String service);

        long getDataPointCount(String service);
    }

    /**
     * Outcome of evaluating a single service.
     */
    enum AnomalyStatus {
        NORMAL,
        ANOMALY,
        SUPPRESSED
    }

    /**
     * Flags a service as anomalous when its current emission rate exceeds the
     * baseline by at least {@code threshold} times, provided enough data points
     * have been collected to trust the baseline.
     */
    static class AnomalyDetector {

        private final LogMetricsRepository repository;
        private final double threshold;
        private final long minDataPoints;

        AnomalyDetector(LogMetricsRepository repository, double threshold, long minDataPoints) {
            this.repository = repository;
            this.threshold = threshold;
            this.minDataPoints = minDataPoints;
        }

        AnomalyStatus evaluate(String service) {
            long dataPoints = repository.getDataPointCount(service);
            if (dataPoints < minDataPoints) {
                return AnomalyStatus.SUPPRESSED;
            }

            double baseline = repository.getBaselineRate(service);
            double current = repository.getCurrentRate(service);

            if (baseline <= 0.0) {
                return current > 0.0 ? AnomalyStatus.ANOMALY : AnomalyStatus.NORMAL;
            }

            return current >= baseline * threshold ? AnomalyStatus.ANOMALY : AnomalyStatus.NORMAL;
        }
    }
}
