package zerowaste.backend.webSocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductWsNotifier {

    private final SimpMessagingTemplate template;

    public ProductWsNotifier(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notifyList(String shareCode, String type, Object payload) {
        String dest = "/topic/notifications/" + shareCode;

        System.out.println("[WS] convertAndSend dest=" + dest);
        System.out.println("[WS] type=" + type);
        System.out.println("[WS] payloadClass=" + (payload == null ? "null" : payload.getClass().getName()));
        System.out.println("[WS] payload=" + payload);
        template.convertAndSend("/topic/notifications/" + shareCode,
                new WsEnvelope(type, payload));
    }

}
