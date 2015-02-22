package com.bytestorm.isp;
import org.mortbay.log.Logger;

public class SimpleJettyLogger implements Logger {

    public SimpleJettyLogger() {
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public void setDebugEnabled(boolean b) {
        debug = b;
    }

    public void info(String msg, Object arg0, Object arg1) {
        if (ENABLED) {
            Log.v("Jetty:  " + format(msg, arg0, arg1));
        }
    }

    public void debug(String msg, Throwable th) {
        if (ENABLED) {
            if (debug) {
                Log.d("Jetty: " + msg);
                Log.d(th.toString());
            }
        }
    }

    public void debug(String msg, Object arg0, Object arg1) {
        if (ENABLED) {
            if (debug) {
                Log.d("Jetty: " + format(msg, arg0, arg1));
            }
        }
    }

    public void warn(String msg, Object arg0, Object arg1) {
        if (ENABLED) {
            Log.d("Jetty: " + format(msg, arg0, arg1));
        }
    }

    public void warn(String msg, Throwable th) {
        if (ENABLED) {
            Log.d("Jetty: " + msg);
            Log.d(th.toString());
        }
    }

    private String format(String msg, Object arg0, Object arg1) {
        int i0 = msg.indexOf("{}");
        int i1 = i0 < 0 ? -1 : msg.indexOf("{}", i0 + 2);

        if (arg1 != null && i1 >= 0)
            msg = msg.substring(0, i1) + arg1 + msg.substring(i1 + 2);
        if (arg0 != null && i0 >= 0)
            msg = msg.substring(0, i0) + arg0 + msg.substring(i0 + 2);
        return msg;
    }

    public Logger getLogger(String name) {
        return this;
    }

    private boolean debug = false;
    private static final boolean ENABLED = false;
}
