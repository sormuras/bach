/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.internal;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Utility classes commonly useful in concurrent programming. */
public class Concurrency {

  // https://github.com/openjdk/loom/blob/7a081efa66f3779bc9e7db2c54930c52fd3b4823/src/java.base/share/classes/java/util/concurrent/ExecutorService.java#L378-L424
  public static class AutoCloseableExecutorService implements AutoCloseable, ExecutorService {

    private final ExecutorService service;

    public AutoCloseableExecutorService(ExecutorService service) {
      this.service = service;
    }

    @Override
    public void close() {
      boolean terminated = isTerminated();
      if (!terminated) {
        shutdown();
        boolean interrupted = false;
        while (!terminated) {
          try {
            terminated = awaitTermination(1L, TimeUnit.DAYS);
          } catch (InterruptedException e) {
            if (!interrupted) {
              shutdownNow(); // interrupt running tasks
              interrupted = true;
            }
          }
        }
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    @Override
    public void shutdown() {
      service.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
      return service.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
      return service.isShutdown();
    }

    @Override
    public boolean isTerminated() {
      return service.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return service.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
      return service.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
      return service.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
      return service.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
      return service.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
      return service.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
      return service.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return service.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
      service.execute(command);
    }
  }

  public static AutoCloseableExecutorService shutdownOnClose(ExecutorService service) {
    return new AutoCloseableExecutorService(service);
  }
}
