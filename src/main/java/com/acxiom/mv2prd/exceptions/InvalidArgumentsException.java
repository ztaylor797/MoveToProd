package com.some.company.mv2prd.exceptions;

import java.util.List;

public class InvalidArgumentsException extends Exception {

    public InvalidArgumentsException(String message) {
        super(message);
    }

    public InvalidArgumentsException(String message, List<String> invalidArgumentList, Throwable err) {
        super(convertInvalidArgListToString(message, invalidArgumentList), err);
    }

    public InvalidArgumentsException(List<String> invalidArgumentList, Throwable err) {
        super(convertInvalidArgListToString(invalidArgumentList), err);
    }

    public InvalidArgumentsException(String message, List<String> invalidArgumentList) {
        super(convertInvalidArgListToString(message, invalidArgumentList));
    }

    public InvalidArgumentsException(List<String> invalidArgumentList) {
        super(convertInvalidArgListToString(invalidArgumentList));
    }

    public static String convertInvalidArgListToString(List<String> invalidArgumentList) {
        return convertInvalidArgListToString("Invalid arguments :::", invalidArgumentList);
    }

    public static String convertInvalidArgListToString(String message, List<String> invalidArgumentList) {
        StringBuilder sb = new StringBuilder(message);
        for (String invalidArgument : invalidArgumentList)
            sb.append(" " + invalidArgument);
        return sb.toString();
    }
}
