package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.DeeplinkRoute;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DeeplinkValidator {

    private static final String DEEPLINK_PREFIX = "wtcapp://";

    private static final List<DeeplinkRouteInfo> VALID_ROUTES = List.of(
        new DeeplinkRouteInfo("wtcapp://home", Pattern.compile("^wtcapp://home$"), "Tela inicial do app"),
        new DeeplinkRouteInfo("wtcapp://profile/{customerId}", Pattern.compile("^wtcapp://profile/[^/]+$"), "Perfil do cliente"),
        new DeeplinkRouteInfo("wtcapp://events/{eventId}", Pattern.compile("^wtcapp://events/[^/]+$"), "Página de evento"),
        new DeeplinkRouteInfo("wtcapp://campaigns/{campaignId}", Pattern.compile("^wtcapp://campaigns/[^/]+$"), "Detalhe de campanha"),
        new DeeplinkRouteInfo("wtcapp://inbox", Pattern.compile("^wtcapp://inbox$"), "Histórico de mensagens"),
        new DeeplinkRouteInfo("wtcapp://purchase/{purchaseId}", Pattern.compile("^wtcapp://purchase/[^/]+$"), "Detalhe de compra")
    );

    public void validateDeeplink(String deeplink) {
        if (deeplink == null || deeplink.isBlank()) {
            return;
        }
        if (!deeplink.startsWith(DEEPLINK_PREFIX)) {
            return;
        }
        if (!isValidRoute(deeplink)) {
            throw new IllegalArgumentException("Deeplink inválido: rota não reconhecida — " + deeplink);
        }
    }

    public void validateActionUrls(Map<String, String> actionUrls) {
        if (actionUrls == null || actionUrls.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : actionUrls.entrySet()) {
            String url = entry.getValue();
            if (url != null && url.startsWith(DEEPLINK_PREFIX)) {
                if (!isValidRoute(url)) {
                    throw new IllegalArgumentException("Deeplink inválido: rota não reconhecida — " + url);
                }
            }
        }
    }

    private boolean isValidRoute(String deeplink) {
        return VALID_ROUTES.stream().anyMatch(route -> route.pattern().matcher(deeplink).matches());
    }

    public List<DeeplinkRoute> getRoutes() {
        return VALID_ROUTES.stream()
                .map(r -> new DeeplinkRoute(r.patternString(), r.description()))
                .toList();
    }

    private record DeeplinkRouteInfo(String patternString, Pattern pattern, String description) {}
}
