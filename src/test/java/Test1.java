import hex.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class Test1 {
    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/main/resources/ApplicationProperties.properties"));
        HEXEditor hexEditor = new HEXEditor(properties);
        hexEditor.run();
    }
}
