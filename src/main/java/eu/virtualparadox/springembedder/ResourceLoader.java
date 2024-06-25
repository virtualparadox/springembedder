package eu.virtualparadox.springembedder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceLoader {

    private ResourceLoader() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Reads a resource file from the classpath and returns it as a string.
     *
     * @param resourcePath the path to the resource file
     * @return the content of the file as a string
     * @throws IOException if there is an error reading the file
     */
    public static String loadResourceAsString(String resourcePath) throws IOException {
        try (final InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            final StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString();
        }
    }
}
