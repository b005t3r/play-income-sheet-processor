package com.bytestorm.isp;

public class Log {
    public static final boolean ENABLE = true;
    
    public static void d(String str) {
        if (ENABLE) {
            System.out.println(str);
        }
    }
    
    public static void v(String str) {
        if (ENABLE) {
            if (verbose) {
                System.out.println(str);
            }
        }
    }
    
    public static void setVerbose(boolean b) {
        verbose = b;
    }
    

    private Log() {
    }
    
    private static boolean verbose;    
}
