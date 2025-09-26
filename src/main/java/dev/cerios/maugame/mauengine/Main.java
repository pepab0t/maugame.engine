package dev.cerios.maugame.mauengine;

import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class Main {
    public static void main(String[] args) throws MauEngineBaseException {
        try (ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory())) {
            executor.schedule(() -> out.println("hello world"), 500, TimeUnit.MILLISECONDS);
            executor.schedule(() -> out.println("hello world"), 1000, TimeUnit.MILLISECONDS);
        };
    }
}