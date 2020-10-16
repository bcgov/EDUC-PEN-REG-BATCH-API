package ca.bc.gov.educ.penreg.api.util;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadFactoryBuilder {

  private String nameFormat;
  private Boolean daemonThread;
  private Integer priority;
  private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;
  private ThreadFactory backingThreadFactory = null;

  /**
   * Returns new {@code ThreadFactory} builder.
   */
  public static ThreadFactoryBuilder create() {
    return new ThreadFactoryBuilder();
  }

  /**
   * Sets the printf-compatible naming format for threads.
   * Use {@code %d} to replace it with the thread number.
   */
  public ThreadFactoryBuilder withNameFormat(final String nameFormat) {
    this.nameFormat = nameFormat;
    return this;
  }

  /**
   * Sets if new threads will be daemon.
   */
  public ThreadFactoryBuilder withDaemon(final boolean daemon) {
    this.daemonThread = daemon;
    return this;
  }

  /**
   * Sets the threads priority.
   */
  public ThreadFactoryBuilder withPriority(final int priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Sets the {@code UncaughtExceptionHandler} for new threads created.
   */
  public ThreadFactoryBuilder withUncaughtExceptionHandler(
      final Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {

    this.uncaughtExceptionHandler = Objects.requireNonNull(uncaughtExceptionHandler);
    return this;
  }

  /**
   * Sets the backing {@code ThreadFactory} for new threads. Threads
   * will be created by invoking {@code newThread(Runnable} on this backing factory.
   */
  public ThreadFactoryBuilder withBackingThreadFactory(final ThreadFactory backingThreadFactory) {
    this.backingThreadFactory = Objects.requireNonNull(backingThreadFactory);
    return this;
  }

  /**
   * Returns a new thread factory using the options supplied during the building process. After
   * building, it is still possible to change the options used to build the ThreadFactory and/or
   * build again.
   */
  public ThreadFactory get() {
    return get(this);
  }

  private static ThreadFactory get(final ThreadFactoryBuilder builder) {
    final String nameFormat = builder.nameFormat;
    final Boolean daemon = builder.daemonThread;
    final Integer priority = builder.priority;
    final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;

    final ThreadFactory backingThreadFactory =
        (builder.backingThreadFactory != null)
            ? builder.backingThreadFactory
            : Executors.defaultThreadFactory();

    final AtomicLong count = (nameFormat != null) ? new AtomicLong(0) : null;

    return runnable -> {
      final Thread thread = backingThreadFactory.newThread(runnable);
      if (nameFormat != null) {
        final String name = String.format(nameFormat, count.getAndIncrement());

        thread.setName(name);
      }
      if (daemon != null) {
        thread.setDaemon(daemon);
      }
      if (priority != null) {
        thread.setPriority(priority);
      }
      if (uncaughtExceptionHandler != null) {
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
      }
      return thread;
    };
  }

}
