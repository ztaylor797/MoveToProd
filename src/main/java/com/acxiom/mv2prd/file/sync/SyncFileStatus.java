package com.some.company.mv2prd.file.sync;

public enum SyncFileStatus {
    SYNCED, PENDING, SCM_ONLY, TAR_ONLY, DIFF, COMP_FAILED, MISSING_BOTH;

    @Override
    public String toString() {
        switch(this) {
            case SYNCED: return "SYNCED";
            case PENDING: return "PENDING";
            case SCM_ONLY: return "SCM_ONLY";
            case TAR_ONLY: return "TAR_ONLY";
            case DIFF: return "DIFF";
            case COMP_FAILED: return "COMP_FAILED";
            case MISSING_BOTH: return "MISSING_BOTH";
            default: throw new IllegalArgumentException();
        }
    }
}
