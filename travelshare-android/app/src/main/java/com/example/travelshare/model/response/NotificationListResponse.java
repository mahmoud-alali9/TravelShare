package com.example.travelshare.model.response;

import com.example.travelshare.model.Notification;
import java.util.List;

public class NotificationListResponse {

    // Liste des notifications de l'utilisateur connecté
    private List<Notification> notifications;

    // Nombre de notifications non lues
    private int unreadCount;

    // Getters
    public List<Notification> getNotifications() { return notifications; }
    public int                getUnreadCount()   { return unreadCount; }
}