package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final IUserRepository userRepository;
    private final PushDevices devices;

    public UserService(IUserRepository userRepository, PushDevices devices) {
        this.userRepository = userRepository;
        this.devices = devices;
    }

    public void updateFcmToken(String email, String fcmToken) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        devices.register(email, fcmToken);
    }
}
