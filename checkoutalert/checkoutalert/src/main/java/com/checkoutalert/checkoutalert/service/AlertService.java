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

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final GeminiService geminiService;
    private final IncidentRepository incidentRepo;

    public AlertService(@Autowired(required = false) JavaMailSender mailSender,
                        RedisTemplate<String, String> redisTemplate,
                        GeminiService geminiService,
                        IncidentRepository incidentRepo) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.geminiService = geminiService;
        this.incidentRepo = incidentRepo;
    }

    private boolean isRateLimited(String endpointId) {
        String key = "alert_sent:" + endpointId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) return true;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(10));
        return false;
    }

    public void sendAlert(MonitoredEndpoint endpoint, long latencyMs,
                          int statusCode, double baseline) {
        // get Gemini diagnosis
        String diagnosis = geminiService.getPossibleCause(
                endpoint.getName(), endpoint.getUrl(),
                latencyMs, baseline, statusCode
        );

        // always save incident to DB regardless of rate limit
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

        // rate limit only the email — not the DB save
        if (isRateLimited(endpoint.getId())) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(endpoint.getAlertEmail());
        message.setSubject("🚨 CheckoutAlert — " + endpoint.getName() + " is DOWN");
        message.setText(
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "CHECKOUTALERT\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Endpoint : " + endpoint.getName() + "\n" +
                        "URL      : " + endpoint.getUrl() + "\n" +
                        "Status   : " + statusCode +
                        " (expected " + endpoint.getExpectedStatus() + ")\n" +
                        "Latency  : " + latencyMs + "ms" +
                        (baseline > 0 ? " (normal: " + (int) baseline + "ms)" : "") + "\n" +
                        "Time     : " + LocalDateTime.now() + "\n\n" +
                        "Possible cause:\n" + diagnosis + "\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "Sent via CheckoutAlert\n"
        );
        mailSender.send(message);
    }
}