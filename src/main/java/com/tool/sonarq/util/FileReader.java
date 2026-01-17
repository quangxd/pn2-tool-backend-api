package com.tool.sonarq.util;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

@Component
public class FileReader {

    public String getResponseInResources(String responseName) {
        try {
            var inputStream = getClass().getClassLoader()
                    .getResourceAsStream(format("simulator/%s", responseName));
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuffer stringBuffer = new StringBuffer();
            while ((line = buffReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            buffReader.close();
            inputStream.close();
            return stringBuffer.toString();
        } catch (IOException ex) {
            throw new IllegalArgumentException();
        }
    }

    public String getResponse(String responseName) {
        try {
            Path filePath = Paths.get(format("/simulator/%s", responseName));
            var inputStream =Files.newInputStream(filePath);
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuffer stringBuffer = new StringBuffer();
            while ((line = buffReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            buffReader.close();
            inputStream.close();
            return stringBuffer.toString();
        } catch (IOException ex) {
            return getResponseInResources(responseName);
        }
    }

}
