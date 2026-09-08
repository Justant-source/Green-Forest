package com.vgc.dto.request;

public class VisibilityRequest {
    private boolean hiddenFromUsers;

    public boolean isHiddenFromUsers() { return hiddenFromUsers; }
    public void setHiddenFromUsers(boolean hiddenFromUsers) { this.hiddenFromUsers = hiddenFromUsers; }
}
