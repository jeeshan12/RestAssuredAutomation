package com.framework.listeners;

import com.framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.concurrent.atomic.AtomicInteger;


public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRIES = ConfigManager.getInstance().getInt("retry.max.attempts", 2);
    private static final long BACKOFF_MS = ConfigManager.getInstance().getLong("retry.backoff.ms", 2000);

    private final AtomicInteger attempt = new AtomicInteger(0);

    @Override
    public boolean retry(ITestResult result) {
        if (attempt.get() >= MAX_RETRIES) {
            return false;
        }
        int currentAttempt = attempt.incrementAndGet();
        log.warn("Retrying {} (attempt {}/{}) after failure: {}",
                result.getMethod().getMethodName(), currentAttempt, MAX_RETRIES,
                result.getThrowable() != null ? result.getThrowable().getMessage() : "unknown error");
        sleepBeforeRetry(currentAttempt);
        return true;
    }

    private void sleepBeforeRetry(int currentAttempt) {
        try {
            Thread.sleep(BACKOFF_MS * currentAttempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
