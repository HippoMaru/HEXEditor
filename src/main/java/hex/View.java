package hex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.OptionalInt;

@Component
public class View {

    private final JFrame jFrame;
    @Autowired
    public View(@Value("${application.width}") int width, @Value("${application.height}") int height){
        jFrame = new JFrame();
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        jFrame.setBounds((dimension.width - width)/2, (dimension.height - height)/2, width, height); //mid loc
        jFrame.setTitle("HEX Editor 1.0");
    }
    public void printModel(ArrayList<ArrayList<Byte>> data){
        for(ArrayList<Byte> byteLine : data){
            for(Byte b : byteLine){
                System.out.printf("%02X ", b);
            }
            System.out.println();
        }
    }

    public void updateTable(ArrayList<ArrayList<Byte>> data){
        OptionalInt columnsNumber = data.stream().mapToInt(ArrayList::size).max();
        JTable jTable = new JTable(data.size(), columnsNumber.getAsInt());
        for(int i=0; i<data.size(); i++){
            for(int j=0; j<data.get(i).size(); j++){
                jTable.setValueAt(String.format("%02X", data.get(i).get(j)), i, j);
            }
        }
        jFrame.add(jTable);
        jFrame.show();
    }
}
