package hex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

@Component
public class Model {
    private final ArrayList<ArrayList<Byte>> data = new ArrayList<>();

    private final String filePath;
    @Autowired
    public Model(@Value("${data.filepath}") String filePath) throws IOException {
        this.filePath = filePath;
        //data loading
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try(BufferedInputStream is =
                    (BufferedInputStream) classloader.getResourceAsStream(filePath)){
            byte i;
            int j = 0;
            data.add(new ArrayList<>());
            while(true){
                assert is != null;
                if ((i = (byte) is.read()) == -1) break; //checks if EOF
                data.get(j).add(i);
                if (i == 10){ //checks if line break
                    data.add(new ArrayList<>());
                    j++;
                }
            }
        }
    }

    public ArrayList<ArrayList<Byte>> getData(){
        return data;
    }

    public void updateFile() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File file = new File(Objects.requireNonNull(classloader.getResource(filePath)).getFile());
        try(FileWriter fileWriter = new FileWriter(file);){
            for (ArrayList<Byte> byteLine : data)
                for (byte b : byteLine) fileWriter.write(b);
            fileWriter.flush();
        }
    }
}
