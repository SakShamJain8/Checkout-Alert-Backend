package com.checkoutalert.checkoutalert.service;

import com.checkoutalert.checkoutalert.model.Incident;
import com.checkoutalert.checkoutalert.model.MonitoredEndpoint;
import com.checkoutalert.checkoutalert.repository.IncidentRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AlertService {

    private JavaMailSender mailSender;
    private RedisTemplate<String, String> redisTemplate;

    private final GeminiService geminiService;
    private final IncidentRepository incidentRepo;

    public AlertService(GeminiService geminiService,
                        IncidentRepository incidentRepo) {
        this.geminiService = geminiService;
        this.incidentRepo = incidentRepo;
    }

    // OPTIONAL INJECTION (SAFE)
    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Autowired(required = false)
    public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private boolean isRateLimited(String endpointId) {
        if (redisTemplate == null) return false; // 🔥 prevent crash

        String key = "alert_sent:" + endpointId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) return true;

        redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(10));
        return false;
    }

    public void sendAlert(MonitoredEndpoint endpoint, long latencyMs,
                          int statusCode, double baseline) {

        String diagnosis = geminiService.getPossibleCause(
                endpoint.getName(), endpoint.getUrl(),
                latencyMs, baseline, statusCode
        );

        Incident incident = new Incident();
        incident.setEndpointId(endpoint.getId());
        incident.setEndpointName(endpoint.getName());
        incident.setEndpointUrl(endpoint.getUrl());
        incident.setStatusCode(statusCode);
        incident.setLatencyMs(latencyMs);
        incident.setBaselineMs(baseline);
        incident.setGeminiDiagnosis(diagnosis);
        incident.setDetectedAt(LocalDateTime.now());
        incident.setUserId(endpoint.getUserId());

        incidentRepo.save(incident);

        if (isRateLimited(endpoint.getId())) return;

        if (mailSender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(endpoint.getAlertEmail());
            message.setSubject("🚨 CheckoutAlert — " + endpoint.getName() + " is DOWN");
            message.setText("Alert triggered...");
            mailSender.send(message);
        }
    }
}