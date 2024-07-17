package com.example.webfluxdisruptor;

import lombok.Data;
import reactor.core.publisher.MonoSink;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Data
public class InputEvent {

    private Map<Long, Long> items;
    private MonoSink<String> response;

    public void clear() {
        items = null;
        response = null;
    }
}
