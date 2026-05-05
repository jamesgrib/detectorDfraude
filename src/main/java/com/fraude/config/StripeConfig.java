package com.fraude.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKeyFromProperties;

    @PostConstruct
    public void init() {
        // Prioridad: variable de entorno > properties
        String envKey = System.getenv("STRIPE_SECRET_KEY");
        if (envKey != null && !envKey.isBlank()) {
            Stripe.apiKey = envKey;
        } else if (secretKeyFromProperties != null && !secretKeyFromProperties.isBlank()) {
            Stripe.apiKey = secretKeyFromProperties;
        } else {
            log.warn("Stripe Secret Key no configurada. Los pagos con Stripe no estarán disponibles.");
        }
    }
}
