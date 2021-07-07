package org.clas.fcmon.ec;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JSplitPane;

import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.utils.groups.IndexedList;

public class ECCalibrationEngine extends CalibrationEngine {
   
        public final static int[]       NUM_PADDLES = {23,62,5};
        public final static String[]     LAYER_NAME = {"LEFT","RIGHT"};
        
        CalibrationConstants calib;
        DetectorCollection<CalibrationData>     collection = new DetectorCollection<CalibrationData>() ;  
                
        IndexedList<DataGroup> dataGroups = new IndexedList<DataGroup>(3);
        IndexedList<Double[]>   constants = new IndexedList<Double[]>(3);

        String fileNamePrefix        = "UNKNOWN";
        String fileName              = "UNKNOWN.txt";
        public String filePath       = ".";
        public String outputFileName = "UNKNOWN.txt";
        
        public ECCalibrationEngine() {
            // controlled by calibration application class
        }
        
        public void setCalibPane() {
        }
        
        @Override
        public void dataEventAction(DataEvent event) {
            //System.out.println("HV dataEventAction "+event.getType());

            if (event.getType()==DataEventType.EVENT_START) {
                System.out.println("Event start received in HV");
                resetEventListener();
                processEvent(event);
            }
            else if (event.getType()==DataEventType.EVENT_ACCUMULATE) {
                //System.out.println("Event accum received in HV");
                processEvent(event);
            }
            else if (event.getType()==DataEventType.EVENT_STOP) {
                System.out.println("Event stop received in HV");
                analyze();
            } 
        }

        @Override
        public void timerUpdate() {
            System.out.println("timerUpdate received in HV");
            analyze();
        }

        public void processEvent(DataEvent event) {
            // overridden in calibration step classes
            
        }

        public void analyze() {
            for (int sector = 1; sector <= 6; sector++) {
                for (int layer = 1; layer <= 4; layer++) {
                    int layer_index = layer - 1;
                    for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
                        fit(sector, layer, paddle, 0.0, 0.0);
                    }
                }
            }
            calib.fireTableDataChanged();
        }
        
        public void init(int is1, int is2) {
            
        }
        
        public void fit(int sector, int layer, int paddle,
                double minRange, double maxRange){
            // overridden in calibration application class
        }
        
        public void updateTable(String inputFile) {
            System.out.println("Dummy method");            
        }
        
        public String getFileName(String runno) {
            this.outputFileName = filePath+fileNamePrefix+"_"+runno;
            return this.outputFileName;
        }
        
        // Borrowed from Louise Clarke 
        // https://github.com/louiseclark/clascalib-services/blob/master/src/main/java/org/jlab/calib/services/TOFCalibrationEngine.java
/*
        public String nextFileName(String path, String runno) {

            Date today = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String todayString = dateFormat.format(today);
            String filePrefix = fileNamePrefix + todayString+"."+runno;
            int newFileNum = 0;

            File dir = new File(path);
            File[] filesList = dir.listFiles();

            for (File file : filesList) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.matches(filePrefix + "[.]\\d+[.]txt")) {
                        String fileNumString = fileName.substring(
                                fileName.indexOf('.') + 1,
                                fileName.lastIndexOf('.'));
                        int fileNum = Integer.parseInt(fileNumString);
                        if (fileNum >= newFileNum)
                            newFileNum = fileNum + 1;

                    }
                }
            }

            return path + filePrefix + "." + newFileNum + ".txt";
        }
*/        
        public void loadHV() {
            
        }
        
        public float getMin(float[] array) {
            float[] dum = array.clone();
            Arrays.sort(dum);
            return dum[0];
        }
        
        public float getMax(float[] array) {
            float[] dum = array.clone();
            Arrays.sort(dum);
            return dum[dum.length-1];
        }

        public void save() {
            // overridden in calibration application class
        }
        
        public void customFit(int sector, int layer, int paddle){
            // overridden in calibration application class
        }
        
        @Override
        public List<CalibrationConstants> getCalibrationConstants() {
            return Arrays.asList(calib);
        }

        @Override
        public IndexedList<DataGroup>  getDataGroup(){
            return dataGroups;
        }
        
        public void analyze(int idet, int is1, int is2, int il1, int il2, int ip1, int ip2) {
        }  
        
        public void analyze(int idet, int is1, int is2, int il1, int il2) {
        }  
        
        public void drawPlots(int sector, int layer, int component) {
            
        }
        
        public void showPlots(int sector, int layer) {
            // Overridden in calibration application class
        }

        public boolean isGoodPaddle(int sector, int layer, int paddle) {
            // Overridden in calibration application class
            return true;
        }
        
        public DataGroup getSummary(int sector, int layer) {
            // Overridden in calibration application class
            DataGroup dg = null;
            return dg;
        }

        public void writeDefaultTables(String runno) {
            // TODO Auto-generated method stub
            
        }

}
