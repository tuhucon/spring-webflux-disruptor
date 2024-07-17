package com.example.webfluxdisruptor;

import com.lmax.disruptor.RingBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequiredArgsConstructor
public class HelloController {

    private final RingBuffer<InputEvent> inputRingBuffer;

    @GetMapping("/publish")
    public Mono<String> publicEvent() {
        inputRingBuffer.publishEvent((t, s) ->
                System.out.println(String.format("public event %s with sequence %d in thread %s", t.toString(), s, Thread.currentThread().toString())));
        return Mono.just("OK");
    }

    @GetMapping("/hello")
    public Mono<String> hello() throws ExecutionException, InterruptedException {
        return Mono.create(sink -> {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                sink.success("Hello Tu hu con");
            });
        });
    }

    @GetMapping("/products")
    public Mono<Map<Long, Long>> products(@RequestParam List<Long> ids) {
        Map<Long, Long> items = new HashMap<>();
        for (Long id: ids) {
            items.put(id, Database.instance.getOrDefault(id, 0L));
        }
        return Mono.just(items);
    }

    @PostMapping("/products/disruptor")
    public Mono<String> buyProductsWithDisruptor(@RequestBody Map<Long, Long> items) {
        return Mono.create(sink -> {
            inputRingBuffer.publishEvent((event, seq, eventItems, eventResult) -> {
                System.out.println("public event at seq = " + seq);
                event.setItems(eventItems);
                event.setResponse(eventResult);
            }, items, sink);
        });
    }

    @PostMapping("/products/lock")
    public Mono<String> buyProductsWithLock(@RequestBody Map<Long, Long> items) {
        Map<Long, Long> processedItems = new HashMap<>();
        boolean[] errors = { false };
        //process items
        for (var entry: items.entrySet()) {
            if (Database.instance.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                errors[0] = true;
                break;
            }
            Database.instance.computeIfPresent(entry.getKey(), (k, v) -> {
                if (v < entry.getValue()) {
                    errors[0] = true;
                    return v;
                } else {
                    return  v - entry.getValue();
                }
            });
            if (errors[0] == false) {
                processedItems.put(entry.getKey(), entry.getValue());
            } else {
                break;
            }
        }
        // if error, compensate
        if (errors[0]) {
            for (var entry: processedItems.entrySet()) {
                Database.instance.computeIfPresent(entry.getKey(), (k, v) -> v + entry.getValue());
            }
            return Mono.just(processedItems.toString());
        }
        return Mono.just("OK");
    }

    @PostMapping("/products/vthread/lock")
    public Mono<String> buyProductsWithLockInVThread(@RequestBody Map<Long, Long> items) {
        return Mono.create(sink -> {
            Thread.ofVirtual().start(() -> {
                Map<Long, Long> processedItems = new HashMap<>();
                boolean[] errors = { false };
                //process items
                for (var entry: items.entrySet()) {
                    if (Database.instance.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                        errors[0] = true;
                        break;
                    }
                    Database.instance.computeIfPresent(entry.getKey(), (k, v) -> {
                        if (v < entry.getValue()) {
                            errors[0] = true;
                            return v;
                        } else {
                            return  v - entry.getValue();
                        }
                    });
                    if (errors[0] == false) {
                        processedItems.put(entry.getKey(), entry.getValue());
                    } else {
                        break;
                    }
                }
                // if error, compensate
                if (errors[0]) {
                    for (var entry: processedItems.entrySet()) {
                        Database.instance.computeIfPresent(entry.getKey(), (k, v) -> v + entry.getValue());
                    }
                    sink.success(processedItems.toString());
                }
                sink.success("OK");
            });
        });
    }
}
