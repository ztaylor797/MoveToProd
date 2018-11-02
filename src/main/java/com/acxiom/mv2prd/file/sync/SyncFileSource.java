package com.some.company.mv2prd.file.sync;

public enum SyncFileSource {
    SCM, TAR;

    @Override
    public String toString() {
        switch(this) {
            case SCM: return "SCM";
            case TAR: return "TAR";
            default: throw new IllegalArgumentException();
        }
    }
}
