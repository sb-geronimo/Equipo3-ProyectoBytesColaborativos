package es.bytescolab.msmaintenance.config;

import feign.RequestInterceptor;
import feign.RetryableException;
import feign.Retryer;
import feign.Request.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalKeyInterceptor(
            @Value("${internal.api-key}") String apiKey
    ) {
        return template -> template.header("X-Internal-Key", apiKey);
    }

    @Bean
    public Retryer getOnlyRetryer() {
        return new GetOnlyRetryer();
    }

    static final class GetOnlyRetryer implements Retryer {

        private int remainingAttempts = 1;

        @Override
        public void continueOrPropagate(RetryableException retryableFailure) {
            if (remainingAttempts <= 0
                    || retryableFailure.request().httpMethod() != HttpMethod.GET) {
                throw retryableFailure;
            }
            remainingAttempts--;
            try {
                long retryDelayMillis = 100L;
                if (retryableFailure.retryAfter() != null) {
                    retryDelayMillis = Math.max(0L, retryableFailure.retryAfter());
                }
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException interruptedFailure) {
                Thread.currentThread().interrupt();
                throw retryableFailure;
            }
        }

        @Override
        public Retryer clone() {
            return new GetOnlyRetryer();
        }
    }
}
