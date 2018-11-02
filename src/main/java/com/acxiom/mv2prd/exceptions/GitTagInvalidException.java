package com.some.company.mv2prd.exceptions;

public class GitTagInvalidException extends Exception {
    public GitTagInvalidException(String errorMessage) {
        super(errorMessage);
    }
}
