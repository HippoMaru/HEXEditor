package hex;
//СЕГОДНЯ ДЕЛАЕМ СЁРЧБАР

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;


public class HEXEditor {

    private final String filePath;
    private final JFrame jFrame;
    private enum ToolTipMode {SELECT_ONE, SELECT_TWO, SELECT_FOUR, SELECT_EIGHT}
    private enum ShiftMode {SHIFT, NO_SHIFT}
    private ToolTipMode curToolTipMode;
    private ShiftMode curShiftMode;
    private byte[] copyBuffer;
    private final ArrayList<ArrayList<Byte>> data;
    public HEXEditor(Properties properties) throws IOException {
        copyBuffer = new byte[]{};
        curToolTipMode = ToolTipMode.SELECT_ONE;
        curShiftMode = ShiftMode.SHIFT;
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
                        tip = String.valueOf(fVal);
                    }
                    case SELECT_EIGHT -> {
                        bytes = new byte[8];
                        for (int i = 0; i < 8; i++) {
                            if (realColumnIndex + i < data.get(rowIndex).size())
                                bytes[i] = data.get(rowIndex).get(realColumnIndex + i);
                        }
                        double dVal = ByteBuffer.wrap(bytes).getDouble();
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

        JMenuItem deleteJMI = new JMenuItem("Delete");
        deleteJMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowStart = table.getSelectedRow();
                if (rowStart < 0) return;
                int rowEnd = table.getSelectionModel().getMaxSelectionIndex();
                int colStart = table.convertColumnIndexToModel(
                        table.getSelectedColumn());
                int colEnd = table.convertColumnIndexToModel(
                        table.getColumnModel().getSelectionModel().getMaxSelectionIndex());
                switch (curShiftMode){
                    case NO_SHIFT -> {
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
                    case SHIFT -> {
                        for (int row=rowStart; row<=rowEnd; row++) {
                            for (int col=colStart; col<=colEnd; col++) {
                                try {
                                    data.get(row).remove(colStart);
                                }
                                catch (Throwable ignored){}
                            }
                        }
                    }
                }
            }
        });
        popupMenu.add(deleteJMI);

        JMenuItem copyJMI = new JMenuItem("Copy");
        copyJMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowStart = table.getSelectedRow();
                if (rowStart < 0) return;
                int rowEnd = table.getSelectionModel().getMaxSelectionIndex();
                int colStart = table.convertColumnIndexToModel(
                        table.getSelectedColumn());
                int colEnd = table.convertColumnIndexToModel(
                        table.getColumnModel().getSelectionModel().getMaxSelectionIndex());
                copyBuffer = new byte[(rowEnd - rowStart + 1) * (colEnd - colStart + 1)];
                for (int row=rowStart; row<=rowEnd; row++) {
                    for (int col=colStart; col<=colEnd; col++) {
                        try {
                            copyBuffer[(row-rowStart)*(colEnd-colStart + 1) + col-colStart] = data.get(row).get(col);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        });
        popupMenu.add(copyJMI);

        JMenuItem pasteJMI = new JMenuItem("Paste");
        pasteJMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0 || copyBuffer.length == 0) return;
                int col = table.convertColumnIndexToModel(table.getSelectedColumn());
                switch (curShiftMode){
                    case NO_SHIFT -> {
                        for (byte b : copyBuffer) {
                            if(data.get(row).size() <= col){data.get(row).add(b);}
                            else{data.get(row).set(col, b);}
                            col++;
                        }
                    }
                    case SHIFT -> {
                        for (byte b : copyBuffer) {
                            try {
                                insertOne(b, row, col++);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                }
            }
        });
        popupMenu.add(pasteJMI);

        table.setComponentPopupMenu(popupMenu);

        //control panels
        JPanel toolTipModePanel = new JPanel(new FlowLayout());
        final JCheckBox toolTipCB1 = new JCheckBox("ToolTipMode: BYTE");
        final JCheckBox toolTipCB2 = new JCheckBox("ToolTipMode: INT");
        final JCheckBox toolTipCB4 = new JCheckBox("ToolTipMode: FLOAT");
        final JCheckBox toolTipCB8 = new JCheckBox("ToolTipMode: DOUBLE");

        toolTipCB1.setSelected(true);

        toolTipCB1.addActionListener(e -> {
            if (toolTipCB1.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_ONE;
                toolTipCB2.setSelected(false);
                toolTipCB4.setSelected(false);
                toolTipCB8.setSelected(false);
            }
        });

        toolTipCB2.addActionListener(e -> {
            if (toolTipCB2.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_TWO;
                toolTipCB1.setSelected(false);
                toolTipCB4.setSelected(false);
                toolTipCB8.setSelected(false);
            }
        });

        toolTipCB4.addActionListener(e -> {
            if (toolTipCB4.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_FOUR;
                toolTipCB1.setSelected(false);
                toolTipCB2.setSelected(false);
                toolTipCB8.setSelected(false);
            }
        });

        toolTipCB8.addActionListener(e -> {
            if (toolTipCB8.isSelected()) {
                curToolTipMode = ToolTipMode.SELECT_EIGHT;
                toolTipCB1.setSelected(false);
                toolTipCB2.setSelected(false);
                toolTipCB4.setSelected(false);
            }
        });

        toolTipModePanel.add(toolTipCB1);
        toolTipModePanel.add(toolTipCB2);
        toolTipModePanel.add(toolTipCB4);
        toolTipModePanel.add(toolTipCB8);

        JPanel shiftModePanel = new JPanel(new FlowLayout());
        final JCheckBox shiftCBShift = new JCheckBox("ShiftMode: SHIFT");
        final JCheckBox shiftCBNoShift = new JCheckBox("ShiftMode: NO SHIFT");

        shiftCBShift.setSelected(true);

        shiftCBShift.addActionListener(e -> {
            if (shiftCBShift.isSelected()) {
                curShiftMode = ShiftMode.SHIFT;
                shiftCBNoShift.setSelected(false);
            }
        });

        shiftCBNoShift.addActionListener(e -> {
            if (shiftCBNoShift.isSelected()) {
                curShiftMode = ShiftMode.NO_SHIFT;
                shiftCBShift.setSelected(false);
            }
        });

        shiftModePanel.add(shiftCBShift);
        shiftModePanel.add(shiftCBNoShift);

        //search panel
        JTextField searchTF = new JTextField(30);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            String[] searchInput = searchTF.getText().split(" ");
            ArrayList<Byte> bytesToSearch = new ArrayList<>();
            for(String byteAsString : searchInput){
                try {bytesToSearch.add(Byte.parseByte(byteAsString));}
                catch (Throwable ignored){return;}
            }
            int sublistIndex;
            for(int i=0; i<data.size(); i++){
                sublistIndex = Collections.indexOfSubList(data.get(i), bytesToSearch);
                if (sublistIndex != -1){
                    table.setRowSelectionInterval(i, i);
                    table.setColumnSelectionInterval(sublistIndex, sublistIndex+bytesToSearch.size() - 1);
                    table.scrollRectToVisible(table.getCellRect(i, sublistIndex, true));
                    break;
                }
            }
        });

        //linking GUI elements
        JScrollPane pane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel searchPanel = new JPanel();
        searchPanel.add(searchTF, BorderLayout.EAST);
        searchPanel.add(searchButton, BorderLayout.WEST);
        jFrame.add(searchPanel, BorderLayout.NORTH);
        jFrame.add(pane, BorderLayout.CENTER);
        JPanel controlPanel = new JPanel();
        controlPanel.add(shiftModePanel, BorderLayout.NORTH);
        controlPanel.add(toolTipModePanel, BorderLayout.SOUTH);
        jFrame.add(controlPanel, BorderLayout.SOUTH);
        jFrame.pack();
    }

    static class CustomTableModel extends AbstractTableModel {

        private final ArrayList<ArrayList<Byte>> data;

        public CustomTableModel(ArrayList<ArrayList<Byte>> data){
            this.data = data;
        }

        public int getColumnCount() {
            OptionalInt result = data.stream().mapToInt(ArrayList::size).max();
            return result.isPresent() ? result.getAsInt() : 0;
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
        { return col != 0; }
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

    public void updateOne(Byte b, int row, int col) {
        while (data.get(row).size() <= col){
            data.get(row).add((byte) 0);
        }
        data.get(row).set(col, b);
    }

    public void updateMany(byte[] byteArray, int iStart, int jStart, int iEnd, int jEnd) {
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
    public void deleteMany(int iStart, int jStart, int iEnd, int jEnd) {
        byte [] byteArray = new byte[(iEnd - iStart + 1) * (jEnd - jStart + 1)];
        updateMany(byteArray, iStart, jStart, iEnd, jEnd);
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

//покажи мне ещё раз
//что же там произошло
//и откуда ты такой искалеченный
//столько лет уже прошло
//память собрана в петлю
//детство долбится сквозь щели скворечника
//что запрятано в углу? что скребет изнутри шкаф?
//тенью манит, когда ты в одиночестве
//покажи мне ещё раз
//что же там произошло
//и откуда ты такой искалеченный
