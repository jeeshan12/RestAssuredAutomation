package com.framework.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;


public class SuiteLifecycleListener implements ISuiteListener, ITestListener {

    private static final Logger log = LoggerFactory.getLogger(SuiteLifecycleListener.class);

    @Override
    public void onStart(ISuite suite) {
        log.info("===== Suite started: {} =====", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("===== Suite finished: {} =====", suite.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info(">> START [{}] {}#{}", Thread.currentThread().getName(),
                result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<< PASS  {}#{}", result.getTestClass().getRealClass().getSimpleName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("<< FAIL  {}#{} - {}", result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "no throwable");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("<< SKIP  {}#{}", result.getTestClass().getRealClass().getSimpleName(), result.getMethod().getMethodName());
    }
}
