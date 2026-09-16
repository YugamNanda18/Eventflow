package com.eventflow.admin;

import com.eventflow.auth.ApiKeyRepository;
import com.eventflow.audit.AuditRepository;
import com.eventflow.dlq.DlqRepository;
import com.eventflow.event.EventRepository;
import com.eventflow.outbox.OutboxRepository;
import com.eventflow.simulator.FailureSimulatorService;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import com.eventflow.webhook.WebhookDeliveryRepository;
import com.eventflow.webhook.WebhookEndpointRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final EventRepository eventRepository;
    private final OutboxRepository outboxRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final DlqRepository dlqRepository;
    private final AuditRepository auditRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final FailureSimulatorService failureSimulatorService;

    public DashboardController(EventRepository eventRepository,
                               OutboxRepository outboxRepository,
                               WebhookEndpointRepository webhookEndpointRepository,
                               WebhookDeliveryRepository deliveryRepository,
                               DlqRepository dlqRepository,
                               AuditRepository auditRepository,
                               ApiKeyRepository apiKeyRepository,
                               OrganizationRepository organizationRepository,
                               FailureSimulatorService failureSimulatorService) {
        this.eventRepository = eventRepository;
        this.outboxRepository = outboxRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.dlqRepository = dlqRepository;
        this.auditRepository = auditRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.organizationRepository = organizationRepository;
        this.failureSimulatorService = failureSimulatorService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Organization demoOrg = organizationRepository.findBySlug("democorp").orElse(null);
        String orgId = demoOrg != null ? demoOrg.getId() : "org_democorp";

        long totalEvents = eventRepository.count();
        long pendingOutbox = outboxRepository.countPending();
        long activeWebhooks = webhookEndpointRepository.count();
        long totalDeliveries = deliveryRepository.countTotalDeliveries(orgId);
        long successfulDeliveries = deliveryRepository.countByOrganizationIdAndStatus(orgId, "SUCCESS");
        long dlqCount = dlqRepository.countByOrganizationIdAndStatus(orgId, "UNRESOLVED");

        double successRate = totalDeliveries > 0 ? (double) successfulDeliveries / totalDeliveries * 100 : 100.0;

        model.addAttribute("totalEvents", totalEvents);
        model.addAttribute("pendingOutbox", pendingOutbox);
        model.addAttribute("activeWebhooks", activeWebhooks);
        model.addAttribute("successRate", String.format("%.1f%%", successRate));
        model.addAttribute("dlqCount", dlqCount);
        model.addAttribute("simulator500", failureSimulatorService.isSimulateWebhook500());
        model.addAttribute("simulatorTimeout", failureSimulatorService.isSimulateWebhookTimeout());

        model.addAttribute("recentEvents", eventRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());

        return "dashboard";
    }

    @GetMapping("/events")
    public String eventsPage(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("events", eventRepository.findAll(PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "events/index";
    }

    @GetMapping("/webhooks")
    public String webhooksPage(Model model) {
        Organization demoOrg = organizationRepository.findBySlug("democorp").orElse(null);
        String orgId = demoOrg != null ? demoOrg.getId() : "org_democorp";
        model.addAttribute("endpoints", webhookEndpointRepository.findByOrganizationId(orgId));
        model.addAttribute("deliveries", deliveryRepository.findByOrganizationId(orgId, PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
        return "webhooks/index";
    }

    @GetMapping("/dlq")
    public String dlqPage(Model model) {
        Organization demoOrg = organizationRepository.findBySlug("democorp").orElse(null);
        String orgId = demoOrg != null ? demoOrg.getId() : "org_democorp";
        model.addAttribute("dlqItems", dlqRepository.findByOrganizationId(orgId, PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
        return "dlq/index";
    }

    @GetMapping("/audit")
    public String auditPage(Model model) {
        Organization demoOrg = organizationRepository.findBySlug("democorp").orElse(null);
        String orgId = demoOrg != null ? demoOrg.getId() : "org_democorp";
        model.addAttribute("auditLogs", auditRepository.findByOrganizationId(orgId, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
        return "audit/index";
    }

    @GetMapping("/api-keys")
    public String apiKeysPage(Model model) {
        Organization demoOrg = organizationRepository.findBySlug("democorp").orElse(null);
        String orgId = demoOrg != null ? demoOrg.getId() : "org_democorp";
        model.addAttribute("apiKeys", apiKeyRepository.findByOrganizationId(orgId));
        return "api-keys/index";
    }

    @GetMapping("/simulator")
    public String simulatorPage(Model model) {
        model.addAttribute("simulator500", failureSimulatorService.isSimulateWebhook500());
        model.addAttribute("simulatorTimeout", failureSimulatorService.isSimulateWebhookTimeout());
        model.addAttribute("simulatorConsumer", failureSimulatorService.isSimulateConsumerFailure());
        return "simulator";
    }
}
