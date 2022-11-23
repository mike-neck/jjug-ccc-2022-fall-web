package com.example;

import org.jetbrains.annotations.NotNull;

class DelegateThread extends Thread {

    private final @NotNull Thread delegate;

    DelegateThread(@NotNull Thread delegate) {
        super(delegate.getThreadGroup(), delegate, delegate.getName());
        this.delegate = delegate;
    }

    @Override
    public void run() {
        delegate.run();
    }

    @Override
    public void interrupt() {
        delegate.interrupt();
    }

    @Override
    public boolean isInterrupted() {
        return delegate.isInterrupted();
    }

    @SuppressWarnings("removal")
    @Override
    public int countStackFrames() {
        return delegate.countStackFrames();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public ClassLoader getContextClassLoader() {
        return delegate.getContextClassLoader();
    }

    @Override
    public void setContextClassLoader(ClassLoader cl) {
        delegate.setContextClassLoader(cl);
    }

    @NotNull
    @Override
    public StackTraceElement @NotNull [] getStackTrace() {
        return delegate.getStackTrace();
    }

    @Override
    public long getId() {
        return delegate.getId();
    }

    @NotNull
    @Override
    public State getState() {
        return delegate.getState();
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return delegate.getUncaughtExceptionHandler();
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler ueh) {
        delegate.setUncaughtExceptionHandler(ueh);
    }

    @Override
    public void start() {
        try {
            delegate.start();
        } catch (Throwable e) {
            JdkServer.EXCEPTION_HANDLER.uncaughtException(this, e);
            throw e;
        }
    }
}
