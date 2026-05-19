package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.entities.AuditLog;
import com.fiap.WtcSync.domain.interfaces.IAuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final IAuditLogRepository auditLogRepository;

    public AuditLogService(IAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String entity, String entityId, String performedBy, String details) {
        AuditLog auditLog = new AuditLog(action, entity, entityId, performedBy, details);
        auditLogRepository.save(auditLog);
    }

    public void logError(String action, String entityId, String performedBy, String errorMessage) {
        AuditLog auditLog = new AuditLog(action, "ERROR", entityId, performedBy, errorMessage);
        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> listAll() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> listByEntity(String entity) {
        return auditLogRepository.findByEntity(entity);
    }

    public List<AuditLog> listByUser(String performedBy) {
        return auditLogRepository.findByPerformedBy(performedBy);
    }
}