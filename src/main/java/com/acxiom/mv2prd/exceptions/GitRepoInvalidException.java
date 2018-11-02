package com.some.company.mv2prd.exceptions;

public class GitRepoInvalidException extends Exception {
    public GitRepoInvalidException(String errorMessage) {
        super(errorMessage);
    }
}
