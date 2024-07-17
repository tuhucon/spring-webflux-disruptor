package com.example.webfluxdisruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebfluxDisruptorApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebfluxDisruptorApplication.class, args);
    }

    @PostConstruct
    private void initDatabase() {
        for (int i = 1; i < 1_000_000; i++) {
            Database.instance.put((long) i, 1_000_000_000L); //ThreadLocalRandom.current().nextLong(20L));
        }
    }

    @Bean
    public Disruptor<InputEvent> inputDisruptor() {
        InputEventFactory inputEventFactory = new InputEventFactory();
        InputEventHandler handler = new InputEventHandler();
        Disruptor<InputEvent> disruptor = new Disruptor<>(inputEventFactory, 32768, DaemonThreadFactory.INSTANCE);
        disruptor.handleEventsWith(handler);
        disruptor.start();
        return disruptor;
    }

    @Bean
    public RingBuffer<InputEvent> inputRingBuffer(Disruptor<InputEvent> inputDisruptor) {
        return inputDisruptor.getRingBuffer();
    }
}
