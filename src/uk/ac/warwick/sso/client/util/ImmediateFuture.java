package uk.ac.warwick.sso.client.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implements a future that is immediately available.
 */
public class ImmediateFuture {
    private ImmediateFuture() {
    }

    /**
     * Returns a future instance.
     * 
     * @param value
     *            the value, which may be null.
     * @return the future
     */
    public static <T> Future<T> of(final T value) {
        return new Future<T>() {
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            public boolean isCancelled() {
                return false;
            }

            public boolean isDone() {
                return true;
            }

            public T get() {
                return value;
            }

            public T get(long timeout, TimeUnit unit) {
                return value;
            }
        };
    }

    /**
     * Returns a future instance that produces an error.
     * 
     * @param error
     *            the exception that should be wrapped in an ExecutionException
     *            when {link #get()} is called.
     * @return the future
     */
    public static <T> Future<T> error(final Throwable error) {
        return new Future<T>() {
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            public boolean isCancelled() {
                return false;
            }

            public boolean isDone() {
                return true;
            }

            public T get() throws ExecutionException {
                throw new ExecutionException(error);
            }

            public T get(long timeout, TimeUnit unit) throws ExecutionException {
                throw new ExecutionException(error);
            }
        };
    }
}