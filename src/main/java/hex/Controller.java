package hex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Component
public class Controller {
    private final Model model;
    private final View view;
    @Autowired
    public Controller(Model model, View view){
        this.model = model;
        this.view = view;
    }
    public void run() throws IOException, InterruptedException {
        view.printModel(model.getData());
        TimeUnit.SECONDS.sleep(5);
        updateOne((byte) 3,0,0);
        TimeUnit.SECONDS.sleep(5);
        deleteOne(0, 0);
        TimeUnit.SECONDS.sleep(5);
        deleteMany(3, 3, 7, 7);
        TimeUnit.SECONDS.sleep(5);
        insertOne((byte) 7, 4, 3);
    }

    public void updateOne(Byte b, int i, int j) throws IOException {
        ArrayList<ArrayList<Byte>> data = model.getData();
        data.get(i).set(j, b);
        model.setData(data);
        model.updateFile();
        view.updateTable(data);
    }

    public void updateMany(byte[] byteArray, int iStart, int jStart, int iEnd, int jEnd) throws IOException {
        ArrayList<ArrayList<Byte>> data = model.getData();
        int byteArrayIndex = 0;
        for(int i=iStart;i<=iEnd; i++){
            if(i >= data.size()) break;
            for(int j=jStart; j<=jEnd; j++){
                if(byteArrayIndex == byteArray.length || j >= data.get(i).size()) break;
                data.get(i).set(j, byteArray[byteArrayIndex++]);
            }
        }
        model.setData(data);
        model.updateFile();
        view.updateTable(data);
    }

    public void deleteOne(int i, int j) throws IOException {
        updateOne((byte) 0, i, j);
    }
    public void deleteMany(int iStart, int jStart, int iEnd, int jEnd) throws IOException {
        byte [] byteArray = new byte[(iEnd - iStart + 1) * (jEnd - jStart + 1)];
        updateMany(byteArray, iStart, jStart, iEnd, jEnd);
    }
    public void insertOne(Byte b, int i, int j) throws IOException {
        ArrayList<ArrayList<Byte>> data = model.getData();
        if (data.get(i).size() <= j) deleteMany(data.get(i).size(), j, i, j);
        data.get(i).add(j, b);
        model.setData(data);
        model.updateFile();
        view.updateTable(data);
    }
    public void insertMany(byte[] byteArray, int iStart, int jStart, int iEnd, int jEnd) throws IOException {
        ArrayList<ArrayList<Byte>> data = model.getData();
        int byteArrayIndex = 0;
        for(int i=iStart;i<=iEnd; i++){
            for(int j=jStart; j<=jEnd; j++){
                if(byteArrayIndex == byteArray.length) break;
                data.get(i).add(j, byteArray[byteArrayIndex]);
            }
        }
        model.setData(data);
        model.updateFile();
        view.updateTable(data);
    }
}
