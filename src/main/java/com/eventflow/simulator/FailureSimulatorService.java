package com.eventflow.simulator;

import org.springframework.stereotype.Service;

@Service
public class FailureSimulatorService {

    private boolean simulateWebhook500 = false;
    private boolean simulateWebhookTimeout = false;
    private boolean simulateConsumerFailure = false;

    public boolean isSimulateWebhook500() { return simulateWebhook500; }
    public void setSimulateWebhook500(boolean simulateWebhook500) { this.simulateWebhook500 = simulateWebhook500; }

    public boolean isSimulateWebhookTimeout() { return simulateWebhookTimeout; }
    public void setSimulateWebhookTimeout(boolean simulateWebhookTimeout) { this.simulateWebhookTimeout = simulateWebhookTimeout; }

    public boolean isSimulateConsumerFailure() { return simulateConsumerFailure; }
    public void setSimulateConsumerFailure(boolean simulateConsumerFailure) { this.simulateConsumerFailure = simulateConsumerFailure; }

    public void reset() {
        this.simulateWebhook500 = false;
        this.simulateWebhookTimeout = false;
        this.simulateConsumerFailure = false;
    }
}
