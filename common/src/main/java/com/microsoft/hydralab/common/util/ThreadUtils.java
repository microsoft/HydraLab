// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ThreadUtils {

    public static <T> void doInParallel(List<T> dataList, int threadCount, ParallelTask<T> task) {
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        int size = dataList.size();
        int span = size / threadCount + 1;
        for (int i = 0; i < threadCount; i++) {
            final int round = i;
            new Thread("insertInParallel start: " + round * span) {
                @Override
                public void run() {
                    int start = round * span;
                    int end = start + span;
                    if (end > dataList.size()) {
                        end = dataList.size();
                    }
                    for (int j = start; j < end; j++) {
                        T item = dataList.get(j);
                        try {
                            task.processOne(item, j - start);
                        } catch (Exception e) {
                            task.onError(item, e);
                        }
                    }
                    countDownLatch.countDown();
                }
            }.start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface ParallelTask<T> {
        void processOne(T item, int innerIndex) throws Exception;

        void onError(T item, Exception e);
    }
}
