package com.dispatchsim.common.retry;

import com.dispatchsim.common.exception.DispatchSimException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class RetryExecutor {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    public <T> T execute(String operationName, RetryableSupplier<T> supplier, Set<Class<? extends Throwable>> retryOn) {
        long backoff = INITIAL_BACKOFF_MS;
        Throwable lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return supplier.get();
            } catch (Throwable throwable) {
                lastFailure = throwable;
                if (!shouldRetry(throwable, retryOn) || attempt == MAX_ATTEMPTS) {
                    break;
                }
                log.warn("{} 第 {} 次执行失败，{} ms 后重试: {}", operationName, attempt, backoff, throwable.getMessage());
                sleep(backoff);
                backoff *= 2;
            }
        }

        if (lastFailure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new DispatchSimException(HttpStatus.INTERNAL_SERVER_ERROR, operationName + " 重试失败");
    }

    private boolean shouldRetry(Throwable throwable, Set<Class<? extends Throwable>> retryOn) {
        return retryOn.stream().anyMatch(type -> type.isAssignableFrom(throwable.getClass()));
    }

    private void sleep(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DispatchSimException(HttpStatus.INTERNAL_SERVER_ERROR, "重试等待被中断");
        }
    }

    @FunctionalInterface
    public interface RetryableSupplier<T> {
        T get() throws Throwable;
    }
}
