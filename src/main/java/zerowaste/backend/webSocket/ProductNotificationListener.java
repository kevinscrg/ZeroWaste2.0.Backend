package zerowaste.backend.webSocket;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProductNotificationListener {

    private final ProductWsNotifier notifier;

    public ProductNotificationListener(ProductWsNotifier notifier) {
        this.notifier = notifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductListWsEvents(ProductListWsEvent event) {
        notifier.notifyList(event.shareCode(), event.type(), event.payload());
    }


}
