package com.eventflow.common.config;

import com.eventflow.auth.*;
import com.eventflow.common.util.HashUtils;
import com.eventflow.event.EventType;
import com.eventflow.event.EventTypeRepository;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import com.eventflow.webhook.WebhookEndpoint;
import com.eventflow.webhook.WebhookEndpointRepository;
import com.eventflow.webhook.WebhookSubscription;
import com.eventflow.webhook.WebhookSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final EventTypeRepository eventTypeRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(OrganizationRepository organizationRepository,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           ApiKeyRepository apiKeyRepository,
                           EventTypeRepository eventTypeRepository,
                           WebhookEndpointRepository webhookEndpointRepository,
                           WebhookSubscriptionRepository webhookSubscriptionRepository,
                           PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.eventTypeRepository = eventTypeRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.findBySlug("democorp").isPresent()) {
            log.info("Demo data already seeded, skipping initialization.");
            return;
        }

        log.info("Seeding EventFlow demo organization, users, roles, API keys, and demo webhooks...");

        // Organization
        Organization demoOrg = new Organization("org_democorp", "DemoCorp", "democorp");
        organizationRepository.save(demoOrg);

        // Roles
        Role adminRole = roleRepository.findById("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "ADMIN", "Admin")));
        Role devRole = roleRepository.findById("ROLE_DEVELOPER").orElseGet(() -> roleRepository.save(new Role("ROLE_DEVELOPER", "DEVELOPER", "Developer")));
        Role opRole = roleRepository.findById("ROLE_OPERATOR").orElseGet(() -> roleRepository.save(new Role("ROLE_OPERATOR", "OPERATOR", "Operator")));
        Role viewerRole = roleRepository.findById("ROLE_VIEWER").orElseGet(() -> roleRepository.save(new Role("ROLE_VIEWER", "VIEWER", "Viewer")));

        String encodedPwd = passwordEncoder.encode("Password123!");

        // Users
        User admin = new User("usr_admin", demoOrg, "admin@demo.eventflow", encodedPwd, "Admin", "User");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        User dev = new User("usr_dev", demoOrg, "developer@demo.eventflow", encodedPwd, "Dev", "User");
        dev.setRoles(Set.of(devRole));
        userRepository.save(dev);

        User op = new User("usr_op", demoOrg, "operator@demo.eventflow", encodedPwd, "Op", "User");
        op.setRoles(Set.of(opRole));
        userRepository.save(op);

        User viewer = new User("usr_viewer", demoOrg, "viewer@demo.eventflow", encodedPwd, "Viewer", "User");
        viewer.setRoles(Set.of(viewerRole));
        userRepository.save(viewer);

        // Demo API Key: ef_live_demo1234567890abcdef123456
        String rawApiKey = "ef_live_demo1234567890abcdef123456";
        ApiKey key = new ApiKey();
        key.setId("key_demo");
        key.setOrganization(demoOrg);
        key.setName("Demo Ingestion Key");
        key.setKeyPrefix("ef_live_demo");
        key.setKeyHash(HashUtils.sha256(rawApiKey));
        key.setScopes("events:write,events:read,webhooks:manage");
        key.setCreatedBy(dev.getId());
        apiKeyRepository.save(key);

        // Event Types
        EventType orderType = new EventType("evt_type_order", demoOrg, "order.created", "Triggered when an order is created");
        eventTypeRepository.save(orderType);

        EventType userType = new EventType("evt_type_user", demoOrg, "user.onboarded", "Triggered when a user completes onboarding");
        eventTypeRepository.save(userType);

        // Webhook Endpoint
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId("wh_demo");
        endpoint.setOrganization(demoOrg);
        endpoint.setUrl("http://localhost:8080/api/v1/demo-webhook");
        endpoint.setSecret("whsec_demosecretkey1234567890");
        endpoint.setDescription("Local Demo Webhook Target");
        endpoint.setActive(true);
        webhookEndpointRepository.save(endpoint);

        WebhookSubscription sub = new WebhookSubscription("sub_demo", endpoint, "order.created");
        webhookSubscriptionRepository.save(sub);

        log.info("Demo Data Initialization finished. Credentials: admin@demo.eventflow / Password123!");
    }
}
