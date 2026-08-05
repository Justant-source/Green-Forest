package com.vgc.service;

import com.vgc.entity.SystemSetting;
import com.vgc.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingService {

    public static final String REGISTRATION_OPEN = "registration_open";

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    public boolean isRegistrationOpen() {
        return systemSettingRepository.findById(REGISTRATION_OPEN)
                .map(s -> !"false".equalsIgnoreCase(s.getSettingValue()))
                .orElse(true);
    }

    @Transactional
    public boolean setRegistrationOpen(boolean open) {
        SystemSetting setting = systemSettingRepository.findById(REGISTRATION_OPEN)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey(REGISTRATION_OPEN);
                    return s;
                });
        setting.setSettingValue(open ? "true" : "false");
        systemSettingRepository.save(setting);
        return open;
    }
}
