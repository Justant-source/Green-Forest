package com.vgc.dto.request;

public class CatalogSyncStatusResponse {
    private boolean running;
    private String lastSuccessAt;
    private String lastFailAt;
    private String lastError;
    private String cooldownUntil;
    private Integer itemCount;

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public String getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(String lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public String getLastFailAt() { return lastFailAt; }
    public void setLastFailAt(String lastFailAt) { this.lastFailAt = lastFailAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getCooldownUntil() { return cooldownUntil; }
    public void setCooldownUntil(String cooldownUntil) { this.cooldownUntil = cooldownUntil; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
}
