package me.msaproject.queryservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConsumerBootstrap {

    @Inject
    QueryConsumer consumer;

    void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        consumer.ensureStarted("cdi-app-init"); // o qualsiasi tuo metodo di start
    }
}
