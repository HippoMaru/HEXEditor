package hex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class Controller {
    private final Model model;
    private final View view;
    @Autowired
    public Controller(Model model, View view){
        this.model = model;
        this.view = view;
    }
    public void run() throws IOException {
        view.printModel(model.getData());
        updateOne((byte) 3,0,0);
    }

    public void updateOne(Byte b, int i, int j) throws IOException {
        ArrayList<ArrayList<Byte>> data = model.getData();
        data.get(i).set(j, b);
        model.setData(data);
        model.updateFile();
        view.updateTable(data);
    }

    public void deleteOne(int i, int j) throws IOException {
        updateOne((byte) 0, i, j);
    }
    public void deleteMany(int iStart, int jStart, int iEnd, int jEnd) throws IOException {
        for(int i=iStart; i<=iEnd; i++){
            for(int j=jStart; j<=jEnd; j++){
                deleteOne(i, j);
            }
        }
    }
    public void insertOne(){}
    public void insertMany(){}
}
