package br.jus.tjsc.ai.agent.config;

import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
class MessageSequenceListener extends AbstractMongoEventListener<Object> {

    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis() * 1_000);

    @Override
    public void onBeforeSave(BeforeSaveEvent<Object> event) {
        Document doc = event.getDocument();
        if (doc != null && doc.containsKey("conversationId")) {
            doc.put("seq", COUNTER.incrementAndGet());
        }
    }
}
