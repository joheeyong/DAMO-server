package com.luxrobo.demoapi.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public String sendToToken(String token, String title, String body) throws Exception {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        return FirebaseMessaging.getInstance().send(message);
    }

    public String sendToAll(java.util.List<String> tokens, String title, String body) throws Exception {
        if (tokens.isEmpty()) return "No tokens";

        var messages = tokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build())
                .toList();

        var response = FirebaseMessaging.getInstance().sendEach(messages);
        return "Success: " + response.getSuccessCount() + ", Fail: " + response.getFailureCount();
    }
}
