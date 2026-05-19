package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.FcmTokenDTO;
import com.fiap.WtcSync.application.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gerenciamento de usuários")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/me/fcm-token")
    @Operation(summary = "Atualiza token FCM", description = "Registra ou atualiza o token de push notification do dispositivo")
    public ResponseEntity<?> updateFcmToken(@RequestBody FcmTokenDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        try {
            userService.updateFcmToken(auth.getName(), dto.fcmToken());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
