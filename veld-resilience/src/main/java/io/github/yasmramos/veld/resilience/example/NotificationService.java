package io.github.yasmramos.veld.resilience.example;

import io.github.yasmramos.veld.resilience.annotation.RateLimiter;

public class NotificationService {

    @RateLimiter(name = "emailNotifications", limitForPeriod = 10, limitRefreshPeriodMillis = 1000)
    public String sendEmail(String recipient, String message) {
        return "Email sent to " + recipient + ": " + message;
    }

    @RateLimiter(name = "smsNotifications", limitForPeriod = 5, limitRefreshPeriodMillis = 1000, timeoutMillis = 500)
    public String sendSms(String phoneNumber, String message) {
        return "SMS sent to " + phoneNumber + ": " + message;
    }

    @RateLimiter(name = "pushNotifications", limitForPeriod = 100, limitRefreshPeriodMillis = 60000)
    public String sendPushNotification(String deviceId, String message) {
        return "Push notification sent to " + deviceId + ": " + message;
    }
}
