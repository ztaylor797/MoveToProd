package com.some.company.mv2prd.main;

public enum Mv2prdMode {
    INSTALL, SYNC_CHECK;

    public static Mv2prdMode getMv2prdModeFromString(String modeString) {
        // (?i:*) -> Ignore case
        if (modeString.matches("(?i:" + Mv2prdMode.INSTALL.toString() + ")")) {
            return Mv2prdMode.INSTALL;
        } else if (modeString.matches("(?i:" + Mv2prdMode.SYNC_CHECK.toString() + ")")) {
            return Mv2prdMode.SYNC_CHECK;
        } else
            return null;
    }

    public String getLogSuffix() {
        return this.toString().toLowerCase();
    }

    @Override
    public String toString() {
        switch(this) {
            case INSTALL: return "INSTALL";
            case SYNC_CHECK: return "SYNC_CHECK";
            default: throw new IllegalArgumentException();
        }
    }
}
