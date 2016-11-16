package org.clas.fcmon.cc;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.groot.graphics.EmbeddedCanvas;

public class CCCalibrationApp extends FCApplication implements CalibrationConstantsListener,ChangeListener {
    
    JPanel                    engineView = new JPanel();
    EmbeddedCanvas                canvas = new EmbeddedCanvas();
    CalibrationConstantsView      ccview = new CalibrationConstantsView();
    ArrayList<CalibrationConstants> list = new ArrayList<CalibrationConstants>();

    public CCCalibrationEngine[] engines = {
            new CCHVEventListener(),
            new CCStatusEventListener(),
            new CCTimingEventListener()
    };

    public final int     HV  = 0;
    public final int STATUS  = 1;
    public final int TIMING  = 2;
    public final int SUMMARY = 3;
    
    String[] names = {"/calibration/ltcc/gain",
                      "/calibration/ltcc/status",
                      "/calibration/ltcc/timing_offset"};
    
    String selectedDir = names[HV];
       
    int selectedSector = 1;
    int selectedLayer = 1;
    int selectedPaddle = 1;
    
    public void init(int is1, int is2) {
        engines[0].init(is1,is2);
        engines[1].init(is1,is2);
        engines[2].init(is1,is2);   
    }
    
    public CCCalibrationApp(String name , CCPixels ccPix) {
        super(name, ccPix);       
     } 
    
    public JPanel getCalibPane() {        
        engineView.setLayout(new BorderLayout());
        JSplitPane enginePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        ccview.getTabbedPane().addChangeListener(this);
        for (int i=0; i < engines.length; i++) {
            ccview.addConstants(engines[i].getCalibrationConstants().get(0),this);
        }        
        enginePane.setRightComponent(canvas);
        enginePane.setLeftComponent(ccview);       
        enginePane.setDividerLocation(0.5);
        engineView.add(enginePane);
        return engineView;       
    }  
    
    public CCCalibrationEngine getSelectedEngine() {
        
        CCCalibrationEngine engine = engines[HV];

        if (selectedDir == names[HV]) {
            engine = engines[HV];
        } else if (selectedDir == names[STATUS]) {
            engine = engines[STATUS];
        } else if (selectedDir == names[TIMING]) {
            engine = engines[TIMING];
        } else if (selectedDir == names[SUMMARY]) {
            engine = engines[SUMMARY];
        } 
        return engine;
    }

    
    public class CCHVEventListener extends CCCalibrationEngine {
        
        public final int[]    EXPECTED_SPE_CHANNEL = {350,350};
        public final int          ALLOWED_SPE_DIFF = 50;
        int is1,is2;
        
        CCHVEventListener(){};
        
        public void init(int is1, int is2) {
            
            this.is1=is1;
            this.is2=is2;
            
            calib = new CalibrationConstants(3,"gain/F");
            calib.setName("/calibration/ltcc/gain");
            calib.setPrecision(3);
            
            for (int il=1 ; il<3; il++) {
                calib.addConstraint(3, EXPECTED_SPE_CHANNEL[il-1]-ALLOWED_SPE_DIFF,
                                       EXPECTED_SPE_CHANNEL[il-1]+ALLOWED_SPE_DIFF);
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setDoubleValue(1.0,"gain",is,il,ip);
                    }
                }
            }
            list.add(calib);
        }
     
        public List<CalibrationConstants>  getCalibrationConstants(){
            return list;
        }  
        
        @Override
        public void analyze() {
            for (int sector = is1; sector < is2; sector++) {
                for (int layer = 1; layer < 3; layer++) {
                    for (int paddle = 1; paddle<NUM_PADDLES[layer-1]+1; paddle++) {
                        fit(sector, layer, paddle, 0.0, 0.0);
                    }
                }
            }
            calib.fireTableDataChanged();
        }
        
        @Override
        public void fit(int sector, int layer, int paddle, double minRange, double maxRange){ 
           double mean = ccPix.strips.hmap2.get("H2_CCa_Hist").get(sector,layer,0).sliceY(paddle-1).getMean();
           calib.addEntry(sector, layer, paddle);
           calib.setDoubleValue(mean, "gain", sector, layer, paddle);
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {
            int rowCount = (sector-1)*36+layer*18+paddle;
            return calib.isValid(rowCount, 3);
        }
    }
    
    private class CCStatusEventListener extends CCCalibrationEngine {
        
        public final int[]    EXPECTED_STATUS = {0,0};
        public final int  ALLOWED_STATUS_DIFF = 1;
        
        CCStatusEventListener(){};
        
        public void init(int is1, int is2){
            calib = new CalibrationConstants(3,"status/I");
            calib.setName("/calibration/ltcc/status");
            calib.setPrecision(3);
            
            for (int i=0 ; i<2; i++) {
                calib.addConstraint(3, EXPECTED_STATUS[i]-ALLOWED_STATUS_DIFF,
                                       EXPECTED_STATUS[i]+ALLOWED_STATUS_DIFF);
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setIntValue(0,"status",is,il,ip);
                    }
                }
            }
            list.add(calib);
        }
        
    }
    
    private class CCTimingEventListener extends CCCalibrationEngine {
        
        public final int[]    EXPECTED_TIMING = {0,0};
        public final int  ALLOWED_TIMING_DIFF = 1;
        
        CCTimingEventListener(){};
        
        public void init(int is1, int is2) {
            calib = new CalibrationConstants(3,"offset/F");
            calib.setName("/calibration/ltcc/timing_offset");
            calib.setPrecision(3);
            
            for (int i=0 ; i<2; i++) {
                calib.addConstraint(3, EXPECTED_TIMING[i]-ALLOWED_TIMING_DIFF,
                                       EXPECTED_TIMING[i]+ALLOWED_TIMING_DIFF);
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setDoubleValue(0.0,"offset",is,il,ip);
                    }
                }
            }
            list.add(calib);
        }
                
    }
           
    public void updateDetectorView(DetectorShape2D shape) {
        CCCalibrationEngine engine = getSelectedEngine();
        DetectorDescriptor dd = shape.getDescriptor();
        this.getDetIndices(dd);
        layer = lay;
        if (this.omap==3) {
           if(engine.isGoodPaddle(is, layer-1, ic)) {
               shape.setColor(101, 200, 59);
           } else {
               shape.setColor(225, 75, 60);
           }
        }
    }
        
    public void constantsEvent(CalibrationConstants cc, int col, int row) {

        String str_sector    = (String) cc.getValueAt(row, 0);
        String str_layer     = (String) cc.getValueAt(row, 1);
        String str_component = (String) cc.getValueAt(row, 2);
            
        if (cc.getName() != selectedDir) {
            selectedDir = cc.getName();
        }
            
        selectedSector = Integer.parseInt(str_sector);
        selectedLayer  = Integer.parseInt(str_layer);
        selectedPaddle = Integer.parseInt(str_component);

    }

        /*
        public void updateCanvas() {

            IndexedList<DataGroup> group = getSelectedEngine().getDataGroup();
            
            if(group.hasItem(selectedSector,selectedLayer,selectedPaddle)==true){
                DataGroup dataGroup = group.getItem(selectedSector,selectedLayer,selectedPaddle);
                this.canvas.draw(dataGroup);
                this.canvas.update();
            } else {
                System.out.println(" ERROR: can not find the data group");
            }
       
        }   
*/   
    public void stateChanged(ChangeEvent e) {
        int i = ccview.getTabbedPane().getSelectedIndex();
        String tabTitle = ccview.getTabbedPane().getTitleAt(i);
        if (tabTitle != selectedDir) {
            selectedDir = tabTitle;
        }
    }
 
}
