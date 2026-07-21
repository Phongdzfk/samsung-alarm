package com.example.samsung_alarm.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    public static final ExecutorService DB = Executors.newSingleThreadExecutor();
    private AppExecutors() {}
}
