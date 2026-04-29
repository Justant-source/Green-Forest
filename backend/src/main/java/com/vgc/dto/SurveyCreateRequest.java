package com.vgc.dto;

import java.time.LocalDateTime;

public class SurveyCreateRequest {
    private LocalDateTime closesAt;
    private boolean anonymous;
    private boolean allowOptionAddByUser;
    private boolean allowMultiSelect;
    private boolean notice;

    public LocalDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalDateTime closesAt) { this.closesAt = closesAt; }
    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    public boolean isAllowOptionAddByUser() { return allowOptionAddByUser; }
    public void setAllowOptionAddByUser(boolean allowOptionAddByUser) { this.allowOptionAddByUser = allowOptionAddByUser; }
    public boolean isAllowMultiSelect() { return allowMultiSelect; }
    public void setAllowMultiSelect(boolean allowMultiSelect) { this.allowMultiSelect = allowMultiSelect; }
    public boolean isNotice() { return notice; }
    public void setNotice(boolean notice) { this.notice = notice; }
}
