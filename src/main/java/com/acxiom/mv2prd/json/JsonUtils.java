package com.some.company.mv2prd.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtils {
    public static byte[] readJsonData(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public static Mv2prdHooks convertJsonDataToMv2prdHooks(byte[] jsonData) throws IOException {
        return new ObjectMapper().readValue(jsonData, Mv2prdHooks.class);
    }
}
