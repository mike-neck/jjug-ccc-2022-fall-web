package com.example;

import java.lang.reflect.Modifier;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingStream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaFlightRecorder {

    private static final Logger logger = LoggerFactory.getLogger(JavaFlightRecorder.class);

    private final RecordingStream rs;

    public JavaFlightRecorder(RecordingStream rs) {
        this.rs = rs;
    }

    void start() {
        logger.info("configure Java Flight Recorder");
        configureVirtualThreadPin();
        configure("jdk.VirtualThreadEnd", "V_END");
        configure("jdk.VirtualThreadStart", "V_START");
        configure("jdk.VirtualThreadSubmitFailed", "V_S_FAIL");
        rs.startAsync();
        logger.info("start Java Flight Recorder");
    }

    void configureVirtualThreadPin() {
        configure("jdk.VirtualThreadPinned", "V_PIN");
    }
    void configure(@NotNull String event, @NotNull String name) {
        rs.enable(event);
        rs.onEvent(event, rec -> {
            // VirtualThreadPinned イベントの場合にスタックトレースを出力わよ〜！
            if ("jdk.VirtualThreadPinned".equals(event)) {
                char nr = '\n';
                StringBuilder sb = new StringBuilder("======PIN stack-trace ").append(name).append("======").append(nr);
                for (RecordedFrame frame : rec.getStackTrace().getFrames()) {
                    sb.append("    ");
                    sb.append(frame.getType());
                    sb.append(' ');
                    RecordedMethod method = frame.getMethod();
                    int mod = method.getModifiers();
                    boolean sync = Modifier.isSynchronized(mod);
                    RecordedClass type = method.getType();
                    if (sync) {
                        sb.append("sync ");
                    } else {
                        sb.append(mod).append(' ');
                    }
                    sb.append(type.getName());
                    sb.append('#');
                    sb.append(method.getName());
                    sb.append('(');
                    sb.append(frame.getLineNumber());
                    sb.append(')');
                    sb.append(nr);
                }
                logger.info(sb.toString());
            } else {
                RecordedThread thread = rec.getThread();
                if (thread != null) {
                    long id = thread.getId();
                    long osId = thread.getOSThreadId();
                    String osName = thread.getOSName();
                    long javaId = thread.getJavaThreadId();
                    String javaName = thread.getJavaName();
                    boolean virtual = thread.isVirtual();

                    logger.info("{} id={} virtual={} os=[{}: {}] java=[{}: {}]", name, id, virtual, osId, osName, javaId, javaName);
                } else {
                    logger.info("{} but no thread", name);
                }
            }
        });
    }

    void finish() {
        logger.info("finishing Java Flight Recorder");
        rs.close();
    }
}
