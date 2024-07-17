package com.example.webfluxdisruptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Database {

    public final static Map<Long, Long> instance = new ConcurrentHashMap<>();
    private Database () {

    }
}
