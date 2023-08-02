package hex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

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
}
