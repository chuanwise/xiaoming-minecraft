package com.chuanwise.xiaoming.minecraft.bukkit.configuration;

import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.beans.Transient;
import java.io.*;
import java.util.Objects;
import java.util.function.Supplier;

public class XiaomingFileConfiguration {
    protected static final Yaml YAML = new Yaml();

    @Getter
    @Setter
    File file;

    @Transient
    public File getFile() {
        return file;
    }

    public void save(File file) throws IOException {
        if (!file.isFile()) {
            file.createNewFile();
        }
        try (OutputStream outputStream = new FileOutputStream(file)) {
            YAML.dump(this, new OutputStreamWriter(outputStream));
        }
    }

    public void save() throws IOException {
        save(file);
    }

    public String serialize() {
        return YAML.dump(this);
    }

    public static <T extends XiaomingFileConfiguration> T load(Yaml yaml, File file, Class<T> clazz) throws IOException {
        final T t;
        try (InputStream inputStream1 = new FileInputStream(file)) {
            t = load(yaml, file, clazz);
        }
        if (Objects.nonNull(t)) {
            t.setFile(file);
        }
        return t;
    }

    public static <T extends XiaomingFileConfiguration> T load(File file, Class<T> clazz) throws IOException {
        return load(getConstruableYaml(clazz), file, clazz);
    }

    public static <T extends XiaomingFileConfiguration> T load(InputStream inputStream, Class<T> clazz) throws IOException {
        return load(getConstruableYaml(clazz), inputStream, clazz);
    }

    public static <T extends XiaomingFileConfiguration> T load(Yaml yaml, InputStream inputStream, Class<T> clazz) throws IOException {
        final T t = yaml.loadAs(inputStream, clazz);
        return t;
    }

    public static <T extends XiaomingFileConfiguration> T loadOrProduce(Yaml yaml, InputStream inputStream, Class<T> clazz, Supplier<T> supplier) {
        T t;
        try {
            t = load(yaml, inputStream, clazz);
        } catch (IOException exception) {
            t = null;
        }
        if (Objects.isNull(t)) {
            t = supplier.get();
        }
        return t;
    }

    public static <T extends XiaomingFileConfiguration> T loadOrProduce(InputStream inputStream, Class<T> clazz, Supplier<T> supplier) {
        return loadOrProduce(getConstruableYaml(clazz), inputStream, clazz, supplier);
    }

    public static <T extends XiaomingFileConfiguration> T loadOrProduce(File file, Class<T> clazz, Supplier<T> supplier) {
        return loadOrProduce(getConstruableYaml(clazz), file, clazz, supplier);
    }

    public static <T extends XiaomingFileConfiguration> T loadOrProduce(Yaml yaml, File file, Class<T> clazz, Supplier<T> supplier) {
        T t = null;
        if (file.isFile()) {
            try {
                try (InputStream inputStream = new FileInputStream(file)) {
                    t = yaml.loadAs(inputStream, clazz);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        if (Objects.isNull(t)) {
            t = supplier.get();
        }
        if (Objects.nonNull(t)) {
            t.setFile(file);
        }
        return t;
    }

    protected static Yaml getConstruableYaml(Class<?> clazz) {
        return new Yaml(new CustomClassLoaderConstructor(clazz.getClassLoader()));
    }
}