package com.some.company.mv2prd.util.logging;

public class DebugSettingSingleton {

    private static Logger logger = Logger.getLogger(DebugSettingSingleton.class);

    private static DebugSettingSingleton instance;

    private Boolean debugEnabled = false;

    private DebugSettingSingleton(){}

    public static DebugSettingSingleton getInstance() {
        if (instance == null) {
            instance = new DebugSettingSingleton();
        }
        return instance;
    }

    public void enableDebugLogging() {
        this.debugEnabled = true;
        logger.debug("Debug logging enabled!");
    }

    public Boolean debugEnabled() {
        return debugEnabled;
    }
}
