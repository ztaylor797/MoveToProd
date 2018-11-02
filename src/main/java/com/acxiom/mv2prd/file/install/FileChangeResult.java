package com.some.company.mv2prd.file.install;

public enum FileChangeResult {
    SUCCESS, PENDING, BACKUP_FAILED, CHOWN_FAILED, CHGRP_FAILED, CHMOD_FAILED, COPY_FAILED, DELETE_SKIPPED, DELETE_FAILED, FAILED, NOT_APPLICABLE;

    @Override
    public String toString() {
        switch(this) {
            case SUCCESS: return "SUCCESS";
            case PENDING: return "PENDING";
            case BACKUP_FAILED: return "BACKUP_FAILED";
            case CHOWN_FAILED: return "CHOWN_FAILED";
            case CHGRP_FAILED: return "CHGRP_FAILED";
            case CHMOD_FAILED: return "CHMOD_FAILED";
            case COPY_FAILED: return "COPY_FAILED";
            case DELETE_SKIPPED: return "DELETE_SKIPPED";
            case DELETE_FAILED: return "DELETE_FAILED";
            case FAILED: return "FAILED";
            case NOT_APPLICABLE: return "NOT_APPLICABLE";
            default: throw new IllegalArgumentException();
        }
    }
}
