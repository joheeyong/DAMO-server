package com.luxrobo.demoapi.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FcmService {

    public String sendToToken(String token, String title, String body) throws Exception {
        return sendToToken(token, title, body, Map.of());
    }

    public String sendToToken(String token, String title, String body, Map<String, String> data) throws Exception {
        var builder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        return FirebaseMessaging.getInstance().send(builder.build());
    }

    public String sendToAll(java.util.List<String> tokens, String title, String body) throws Exception {
        return sendToAll(tokens, title, body, Map.of());
    }

    public String sendToAll(java.util.List<String> tokens, String title, String body, Map<String, String> data) throws Exception {
        if (tokens.isEmpty()) return "No tokens";

        var messages = tokens.stream()
                .map(token -> {
                    var builder = Message.builder()
                            .setToken(token)
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build());
                    if (data != null && !data.isEmpty()) {
                        builder.putAllData(data);
                    }
                    return builder.build();
                })
                .toList();

        var response = FirebaseMessaging.getInstance().sendEach(messages);
        return "Success: " + response.getSuccessCount() + ", Fail: " + response.getFailureCount();
    }
}
