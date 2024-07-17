package com.example.webfluxdisruptor;

import com.lmax.disruptor.EventFactory;

public class InputEventFactory implements EventFactory<InputEvent> {

    @Override
    public InputEvent newInstance() {
        return new InputEvent();
    }
}
