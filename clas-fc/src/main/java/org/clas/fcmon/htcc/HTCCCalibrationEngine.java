package org.clas.fcmon.htcc;
import java.util.Arrays;
import java.util.List;

import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.utils.groups.IndexedList;

public class HTCCCalibrationEngine extends CalibrationEngine {
   

        public final static int[]       NUM_PADDLES = {48};
        public final static String[]    LAYER_NAME = {"UP","DN"};
        
        CalibrationConstants calib;
        
        IndexedList<DataGroup> dataGroups = new IndexedList<DataGroup>(3);
        IndexedList<Double[]> constants = new IndexedList<Double[]>(3);


        public HTCCCalibrationEngine() {
            // controlled by calibration application class
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

}
