package hex;


import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;


public class HEXEditor {

    private final String filePath;
    private final JFrame jFrame;
    private enum ToolTipMode {SELECT_ONE, SELECT_TWO, SELECT_FOUR, SELECT_EIGHT;}
    private ToolTipMode curToolTipMode;
    private byte copiedOne;
    private byte[] copiedMany;
    private final ArrayList<ArrayList<Byte>> data;
    public HEXEditor(Properties properties) throws IOException {
        copiedOne = -1;
        copiedMany = new byte[]{};
        curToolTipMode = ToolTipMode.SELECT_ONE;
        //data loading
        this.filePath = properties.getProperty("data.filepath");
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        data = new ArrayList<>();
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


        //JFrame init
        int width = Integer.parseInt(properties.getProperty("application.width"));
        int height = Integer.parseInt(properties.getProperty("application.height"));
        jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        jFrame.setBounds((dimension.width - width)/2, (dimension.height - height)/2, width, height); //mid loc
        jFrame.setTitle("I'm SO tired");
        jFrame.addWindowListener(new WindowAdapter(){

            public void windowClosing(WindowEvent e){
                try {
                    updateFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        });


        //JTable init
        JTable table = new JTable(new CustomTableModel(data)){
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);

                if (rowIndex < 0) return "00";
                byte[] bytes;
                switch (curToolTipMode) {
                    case SELECT_ONE -> {
                        if (data.get(rowIndex).size() <= colIndex) tip = "00";
                        else tip = String.valueOf(data.get(rowIndex).get(realColumnIndex));
                    }
                    case SELECT_TWO -> {
                        bytes = new byte[2];
                        for (int i = 0; i < 2; i++) {
                            if (realColumnIndex + i < data.get(rowIndex).size())
                                bytes[i] = data.get(rowIndex).get(realColumnIndex + i);
                        }
                        int iVal = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
                        tip = String.valueOf(iVal);
                    }
                    case SELECT_FOUR -> {
                        bytes = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            if (realColumnIndex + i < data.get(rowIndex).size())
                                bytes[i] = data.get(rowIndex).get(realColumnIndex + i);
                        }
                        float fVal = ByteBuffer.wrap(bytes).getFloat();
                        ;
                        tip = String.valueOf(fVal);
                    }
                    case SELECT_EIGHT -> {
                        bytes = new byte[8];
                        for (int i = 0; i < 8; i++) {
                            if (realColumnIndex + i < data.get(rowIndex).size())
                                bytes[i] = data.get(rowIndex).get(realColumnIndex + i);
                        }
                        double dVal = ByteBuffer.wrap(bytes).getDouble();
                        ;
                        tip = String.valueOf(dVal);
                    }
                }
                return tip;
            }
        };
        table.setCellSelectionEnabled(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        //popup menu
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem deleteOneNoShiftJMI = new JMenuItem("DeleteOneNoShift");
        deleteOneNoShiftJMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                int col = table.convertColumnIndexToModel(table.getSelectedColumn());
                try {
                    deleteOne(row, col);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        popupMenu.add(deleteOneNoShiftJMI);

        JMenuItem deleteManyNoShiftJMI = new JMenuItem("DeleteManyNoShift");
        deleteManyNoShiftJMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int rowStart = table.getSelectedRow();
                if (rowStart < 0) return;
                int rowEnd = table.getSelectionModel().getMaxSelectionIndex();
                int colStart = table.convertColumnIndexToModel(
                        table.getSelectedColumn());
                int colEnd = table.convertColumnIndexToModel(
                        table.getColumnModel().getSelectionModel().getMaxSelectionIndex());
                for (int row=rowStart; row<=rowEnd; row++) {
                    for (int col=colStart; col<=colEnd; col++) {
                        try {
                            deleteOne(row, col);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        });
        popupMenu.add(deleteManyNoShiftJMI);

        JMenuItem copyOneJMI = new JMenuItem("CopyOne");
        copyOneJMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                int col = table.convertColumnIndexToModel(table.getSelectedColumn());
                copyOne(row, col);
            }
        });
        popupMenu.add(copyOneJMI);

        JMenuItem copyManyJMI = new JMenuItem("CopyMany");
        copyManyJMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int rowStart = table.getSelectedRow();
                if (rowStart < 0) return;
                int rowEnd = table.getSelectionModel().getMaxSelectionIndex();
                int colStart = table.convertColumnIndexToModel(
                        table.getSelectedColumn());
                int colEnd = table.convertColumnIndexToModel(
                        table.getColumnModel().getSelectionModel().getMaxSelectionIndex());
                copiedMany = new byte[(rowEnd - rowStart + 1) * (colEnd - colStart + 1)];
                for (int row=rowStart; row<=rowEnd; row++) {
                    for (int col=colStart; col<=colEnd; col++) {
                        try {
                            copiedMany[(row-rowStart)*(colEnd-colStart + 1) + col-colStart] = data.get(row).get(col);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                System.out.println(Arrays.toString(copiedMany));
            }
        });
        popupMenu.add(copyManyJMI);

        JMenuItem insertOneShiftJMI = new JMenuItem("InsertOneShift");
        insertOneShiftJMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0 || copiedOne == -1) return;
                int col = table.convertColumnIndexToModel(table.getSelectedColumn());
                try {
                    insertOne(copiedOne, row, col);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        popupMenu.add(insertOneShiftJMI);

        JMenuItem insertManyShiftJMI = new JMenuItem("InsertManyShift");
        insertManyShiftJMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0 || copiedMany.length == 0) return;
                int col = table.convertColumnIndexToModel(table.getSelectedColumn());
                for (byte b : copiedMany) {
                    try {
                        insertOne(b, row, col++);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        popupMenu.add(insertManyShiftJMI);

        table.setComponentPopupMenu(popupMenu);

        //control
        JPanel control = new JPanel(new FlowLayout());
        final JCheckBox cb1 = new JCheckBox("ToolTipMode: BYTE");
        final JCheckBox cb2 = new JCheckBox("ToolTipMode: INT");
        final JCheckBox cb4 = new JCheckBox("ToolTipMode: FLOAT");
        final JCheckBox cb8 = new JCheckBox("ToolTipMode: DOUBLE");

        cb1.setSelected(true);

        cb1.addActionListener(e -> {
            if (cb1.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_ONE;
                cb2.setSelected(false);
                cb4.setSelected(false);
                cb8.setSelected(false);
            }
        });

        cb2.addActionListener(e -> {
            if (cb2.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_TWO;
                cb1.setSelected(false);
                cb4.setSelected(false);
                cb8.setSelected(false);
            }
        });

        cb4.addActionListener(e -> {
            if (cb4.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_FOUR;
                cb1.setSelected(false);
                cb2.setSelected(false);
                cb8.setSelected(false);
            }
        });

        cb8.addActionListener(e -> {
            if (cb8.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_EIGHT;
                cb1.setSelected(false);
                cb2.setSelected(false);
                cb4.setSelected(false);
            }
        });

        control.add(cb1);
        control.add(cb2);
        control.add(cb4);
        control.add(cb8);

        //linking
        JScrollPane pane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        ArrayList<String> rowEnum = new ArrayList<>();
//        for (int index = 0; index < data.size(); index++) {
//            rowEnum.add(String.valueOf(index));
//        }
//        JList<String> list = new JList<String>(rowEnum.toArray(new String[0]));
//        pane.setRowHeaderView(list);
        jFrame.add(pane, BorderLayout.CENTER);
        jFrame.add(control, BorderLayout.SOUTH);
        jFrame.pack();
    }

    class CustomTableModel extends AbstractTableModel {

        private final ArrayList<ArrayList<Byte>> data;

        public CustomTableModel(ArrayList<ArrayList<Byte>> data){
            this.data = data;
        }

        public int getColumnCount() {
            return data.stream().mapToInt(ArrayList::size).max().getAsInt();
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return Integer.toString(col);
        }

        public String getValueAt(int row, int col) {
            if (data.get(row).size() <= col) return "00";
            return String.format("%02X", data.get(row).get(col));
        }

        public boolean isCellEditable(int row, int col)
        { return true; }
        public void setValueAt(Object value, int row, int col) {
            while (data.get(row).size() <= col){
                data.get(row).add((byte) 0);
            }
            try {data.get(row).set(col, Byte.valueOf((String) value));}
            catch (Throwable ignored){};
            fireTableCellUpdated(row, col);
        }
    }

    public void run(){
        jFrame.setVisible(true);
    }
    public void updateFile() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File file = new File(Objects.requireNonNull(classloader.getResource(filePath)).getFile());
        try (FileWriter fileWriter = new FileWriter(file);) {
            for (ArrayList<Byte> byteLine : data){
                for (byte b : byteLine) fileWriter.write(b);
            }
            fileWriter.flush();
        }
    }

    public void updateOne(Byte b, int row, int col) throws IOException {
        while (data.get(row).size() <= col){
            data.get(row).add((byte) 0);
        }
        data.get(row).set(col, b);
    }

    public void updateMany(byte[] byteArray, int iStart, int jStart, int iEnd, int jEnd) throws IOException {
        int byteArrayIndex = 0;
        for(int i=iStart;i<=iEnd; i++){
            if(i >= data.size()) break;
            for(int j=jStart; j<=jEnd; j++){
                if(byteArrayIndex == byteArray.length || j >= data.get(i).size()) break;
                data.get(i).set(j, byteArray[byteArrayIndex++]);
            }
        }
    }

    public void deleteOne(int i, int j) throws IOException {
        updateOne((byte) 0, i, j);
    }
    public void deleteMany(int iStart, int jStart, int iEnd, int jEnd) throws IOException {
        byte [] byteArray = new byte[(iEnd - iStart + 1) * (jEnd - jStart + 1)];
        updateMany(byteArray, iStart, jStart, iEnd, jEnd);
    }

    public void copyOne(int i, int j){
        if (data.get(i).size() <= j) copiedOne = 0;
        else copiedOne = data.get(i).get(j);
    }
    public void insertOne(Byte b, int i, int j) throws IOException {
        if (data.get(i).size() <= j) deleteMany(data.get(i).size(), j, i, j);
        data.get(i).add(j, b);

    }

    public void printDataDEBUG(){
        for(ArrayList<Byte> byteLine : data){
            for(Byte b : byteLine){
                System.out.printf("%02X ", b);
            }
            System.out.println();
        }
    }
}
