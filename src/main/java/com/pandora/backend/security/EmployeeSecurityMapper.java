package com.pandora.backend.security;

import com.pandora.backend.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeSecurityMapper {

    private final PhoneSecurityService phoneSecurityService;

    public EmployeeSecurityMapper(final PhoneSecurityService phoneSecurityService) {
        this.phoneSecurityService = phoneSecurityService;
    }

    public void setPhone(final Employee employee, final String plainPhone) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee is null");
        }
        if (plainPhone == null || plainPhone.isBlank()) {
            throw new IllegalArgumentException("Phone is blank");
        }
        String phoneHash = phoneSecurityService.hashPhone(plainPhone);
        String phoneEnc = phoneSecurityService.encryptPhone(plainPhone);
        employee.setPhoneHash(phoneHash);
        employee.setPhoneEnc(phoneEnc);
    }

    public String getPhonePlain(final Employee employee) {
        if (employee == null) {
            return null;
        }
        return phoneSecurityService.decryptPhone(employee.getPhoneEnc());
    }
}
