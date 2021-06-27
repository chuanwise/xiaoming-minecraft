package com.chuanwise.xiaoming.minecraft.server.data;

import com.chuanwise.xiaoming.core.preserve.JsonFilePreservable;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class ServerPlayerData extends JsonFilePreservable {
    Map<Long, ServerPlayer> players = new HashMap<>();

    public ServerPlayer forPlayer(String id) {
        for (ServerPlayer player : players.values()) {
            if (Objects.equals(player.getId(), id)) {
                return player;
            }
        }
        return null;
    }

    public ServerPlayer forPlayer(long code) {
        return players.get(code);
    }

    public void bind(long code, String id) {
        players.put(code, new ServerPlayer(code, id));
    }

    public void unbind(long code) {
        players.remove(code);
    }
}
