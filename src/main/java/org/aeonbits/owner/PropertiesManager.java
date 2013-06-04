/*
 * Copyright (c) 2013, Luigi R. Viggiano
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package org.aeonbits.owner;


import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.Sources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import static org.aeonbits.owner.Config.LoadType;
import static org.aeonbits.owner.Config.LoadType.FIRST;
import static org.aeonbits.owner.ConfigURLStreamHandler.CLASSPATH_PROTOCOL;
import static org.aeonbits.owner.PropertiesMapper.defaults;
import static org.aeonbits.owner.Util.reverse;

/**
 * Loads properties and manages access to properties handling concurrency.
 *
 * @author Luigi R. Viggiano
 */
class PropertiesManager implements Reloadable {
    private static final SystemVariablesExpander expander = new SystemVariablesExpander();
    private final Class<? extends Config> clazz;
    private final Map<?, ?>[] imports;
    private final Properties properties = new Properties();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReadLock readLock = lock.readLock();
    private final WriteLock writeLock = lock.writeLock();


    PropertiesManager(Class<? extends Config> clazz, Map<?, ?>... imports) {
        this.clazz = clazz;
        this.imports = imports;
    }

    Properties load() {
        return load(false);
    }

    public void reload() {
        load(true);
    }

    private Properties load(boolean clear) {
        writeLock.lock();
        try {
            if (clear)
                properties.clear();
            defaults(properties, clazz);
            merge(properties, reverse(imports));
            ConfigURLStreamHandler handler = new ConfigURLStreamHandler(clazz.getClassLoader(), expander);
            Properties loadedFromFile = doLoad(handler);
            merge(properties, loadedFromFile);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    Properties doLoad(ConfigURLStreamHandler handler) throws IOException {
        Sources sources = clazz.getAnnotation(Sources.class);
        LoadPolicy loadPolicy = clazz.getAnnotation(LoadPolicy.class);
        LoadType loadType = (loadPolicy != null) ? loadPolicy.value() : FIRST;
        if (sources == null)
            return loadDefaultProperties(handler);
        else
            return loadType.load(sources, handler);
    }

    private Properties loadDefaultProperties(ConfigURLStreamHandler handler) throws IOException {
        String spec = CLASSPATH_PROTOCOL + ":" + clazz.getName().replace('.', '/') + ".properties";
        InputStream inputStream = getInputStream(new URL(null, spec, handler));
        try {
            return properties(inputStream);
        } finally {
            close(inputStream);
        }
    }

    static InputStream getInputStream(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        if (conn == null)
            return null;
        return conn.getInputStream();
    }

    private static void merge(Properties results, Map<?, ?>... inputs) {
        for (Map<?, ?> input : inputs)
            results.putAll(input);
    }

    static Properties properties(InputStream stream) throws IOException {
        Properties props = new Properties();
        if (stream != null)
            props.load(stream);
        return props;
    }

    static void close(InputStream inputStream) throws IOException {
        if (inputStream != null)
            inputStream.close();
    }

    Properties properties() {
        readLock.lock();
        try {
            return properties;
        } finally {
            readLock.unlock();
        }
    }

    public String getProperty(String key) {
        readLock.lock();
        try {
            return properties.getProperty(key);
        }finally {
            readLock.unlock();
        }
    }
}