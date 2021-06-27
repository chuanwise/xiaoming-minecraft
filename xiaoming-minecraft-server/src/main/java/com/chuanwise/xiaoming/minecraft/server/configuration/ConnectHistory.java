package com.chuanwise.xiaoming.minecraft.server.configuration;

import com.chuanwise.xiaoming.core.preserve.JsonFilePreservable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectHistory extends JsonFilePreservable {
    Map<String, List<Long>> histories = new HashMap<>();

    public void addHistory(String identify) {
        List<Long> longs = histories.get(identify);
        if (Objects.isNull(longs)) {
            longs = new ArrayList<>();
            histories.put(identify, longs);
        }
        longs.add(System.currentTimeMillis());
    }

    public boolean isConnected(String identify) {
        return Objects.nonNull(histories.get(identify));
    }

    public List<Long> forIdentify(String identify) {
        return histories.get(identify);
    }

    public void removeBefore(String identify, long time) {
        final List<Long> times = forIdentify(identify);
        if (Objects.nonNull(times)) {
            times.removeIf(t -> t < time);
            if (times.isEmpty()) {
                histories.remove(identify);
            }
        }
    }

    public void removeBefore(long time) {
        for (Map.Entry<String, List<Long>> entry : histories.entrySet()) {
            removeBefore(entry.getKey(), time);
        }
    }
}
