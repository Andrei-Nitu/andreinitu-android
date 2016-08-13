package com.kii.demo.wearable;

/**
 * Created by bogdan on 11/08/16.
 */
public class Heartbeat {
    public int value;
    public Long timestmap;
    public String created;

    public Heartbeat(int value, Long timestmap, String created) {
        this.value = value;
        this.timestmap = timestmap;
        this.created = created;
    }
}
