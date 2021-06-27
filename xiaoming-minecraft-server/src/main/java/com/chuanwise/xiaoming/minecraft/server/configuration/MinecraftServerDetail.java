package com.chuanwise.xiaoming.minecraft.server.configuration;

import com.chuanwise.xiaoming.api.util.MD5Utils;
import lombok.Data;

import java.util.*;

@Data
public class MinecraftServerDetail {
    String name;
    String identify;
    transient byte[] identifyMd5;
    Set<String> tags = new HashSet<>();

    Map<String, Set<String>> worldTags = new HashMap<>();

    public void setIdentify(String identify) {
        this.identify = identify;
        identifyMd5 = MD5Utils.INSTANCE.getMD5(identify);
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
        tags.addAll(Arrays.asList("recorded", name));
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public boolean hasWorld(String worldName) {
        return worldTags.containsKey(worldName);
    }

    public boolean worldHasTag(String worldName, String tag) {
        if (Arrays.asList(worldName, "recorded").contains(tag)) {
            return true;
        }
        final Set<String> strings = worldTags.get(worldName);
        return Objects.nonNull(strings) && strings.contains(tag);
    }

    public Set<String> forTaggedWorldNames(String tag) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : worldTags.entrySet()) {
            if (entry.getValue().contains(tag)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Set<String> forWorldTags(String tag) {
        return worldTags.get(tag);
    }

    public void addWorldTag(String worldName, String tag) {
        Set<String> worldTags = this.worldTags.get(worldName);
        if (Objects.isNull(worldTags)) {
            worldTags = new HashSet<>();
            this.worldTags.put(worldName, worldTags);
            worldTags.addAll(Arrays.asList("recorded", worldName));
        }
        worldTags.add(tag);
    }

    public void removeWorldTag(String worldName, String tag) {
        final Set<String> strings = worldTags.get(worldName);
        if (Objects.nonNull(strings)) {
            strings.remove(tag);
        }
    }
}