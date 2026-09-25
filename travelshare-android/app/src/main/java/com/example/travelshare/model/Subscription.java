package com.example.travelshare.model;

import com.google.gson.annotations.SerializedName;

public class Subscription {

    @SerializedName("_id")
    private String id;
    private String type;
    private String targetId;
    private String targetName;

    public String getId()         { return id; }
    public String getType()       { return type; }
    public String getTargetId()   { return targetId; }
    public String getTargetName() { return targetName; }

    public String getDisplayName() {
        return (targetName != null && !targetName.isEmpty()) ? targetName : targetId;
    }
}
