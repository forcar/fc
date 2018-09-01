package org.clas.fcmon.band;

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

public class BANDCalibrationApp extends FCApplication implements CalibrationConstantsListener,ChangeListener {
    
    JPanel                    engineView = new JPanel();
    EmbeddedCanvas                canvas = new EmbeddedCanvas();
    CalibrationConstantsView      ccview = new CalibrationConstantsView();
    ArrayList<CalibrationConstants> list = new ArrayList<CalibrationConstants>();

    public BANDCalibrationEngine[] engines = {
            new BANDHVEventListener(),
            new BANDStatusEventListener()
    };

    public final int     HV  = 0;
    public final int STATUS  = 1;
    
    String[] names = {"/calibration/band/gain_balance,/calibration/band/status"};
    
    String selectedDir = names[HV];
       
    int selectedSector = 1;
    int selectedLayer = 1;
    int selectedPaddle = 1;
    
    public void init(int is1, int is2) {
        engines[0].init(is1,is2);
        engines[1].init(is1,is2);   
    }
    
    public BANDCalibrationApp(String name , BANDPixels[] bandPix) {
        super(name, bandPix);       
     } 
    
    public JPanel getCalibPane() {        
        engineView.setLayout(new BorderLayout());
        JSplitPane enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
        ccview.getTabbedPane().addChangeListener(this);
        for (int i=0; i < engines.length; i++) {
            ccview.addConstants(engines[i].getCalibrationConstants().get(0),this);
        }   

        enginePane.setTopComponent(canvas);
        enginePane.setBottomComponent(ccview);       
        enginePane.setResizeWeight(0.8);
        engineView.add(enginePane);
        return engineView;       
    }  
    
    public BANDCalibrationEngine getSelectedEngine() {
        
        BANDCalibrationEngine engine = engines[HV];

        if (selectedDir == names[HV]) {
            engine = engines[HV];
        } else if (selectedDir == names[STATUS]) {
            engine = engines[STATUS];
        } 
        return engine;
    }

    
    public class BANDHVEventListener extends BANDCalibrationEngine {
        
        public final int[]      EXPECTED_MIP_CHANNEL = {800, 2000, 800};
        public final int        ALLOWED_MIP_DIFF = 50;        
        
        int is1,is2;
        
        BANDHVEventListener(){};
        
        public void init(int is1, int is2) {
            
            this.is1=is1;
            this.is2=is2;
            
            calib = new CalibrationConstants(3,
                    "mipa_left/F:mipa_right/F:mipa_left_err/F:mipa_right_err/F:logratio/F:logratio_err/F");
            calib.setName("/calibration/band/gain_balance");
            calib.setPrecision(3);

            for (int i=0; i<3; i++) {
                
                int layer = i+1;
                calib.addConstraint(3, EXPECTED_MIP_CHANNEL[i]-ALLOWED_MIP_DIFF, 
                                       EXPECTED_MIP_CHANNEL[i]+ALLOWED_MIP_DIFF, 1, layer);
                // calib.addConstraint(calibration column, min value, max value,
                // col to check if constraint should apply, value of col if constraint should be applied);
                // (omit last two if applying to all rows)
                calib.addConstraint(4, EXPECTED_MIP_CHANNEL[i]-ALLOWED_MIP_DIFF, 
                                       EXPECTED_MIP_CHANNEL[i]+ALLOWED_MIP_DIFF, 1, layer);
            }
            
            list.add(calib);         
        }
     
        public List<CalibrationConstants>  getCalibrationConstants(){
            return list;
        }  
        
        @Override
        public void analyze() {
            for (int sector = is1; sector < is2; sector++) {
                for (int layer = 1; layer < 4; layer++) {
                    for (int paddle = 1; paddle<NUM_PADDLES[layer-1]+1; paddle++) {
                        fit(sector, layer, paddle, 0.0, 0.0);
                    }
                }
            }
            calib.fireTableDataChanged();
        }
        
        @Override
        public void fit(int sector, int layer, int paddle, double minRange, double maxRange){ 
           double mean = bandPix[layer-1].strips.hmap2.get("H2_a_Hist").get(sector,0,0).sliceY(paddle-1).getMean();
           calib.addEntry(sector, layer, paddle);
           calib.setDoubleValue(mean, "mipa_left", sector, layer, paddle);
           calib.setDoubleValue(mean, "mipa_right", sector, layer, paddle);
        }
        
        public double getMipChannel(int sector, int layer, int paddle) {
            return calib.getDoubleValue("mipa_left", sector, layer, paddle);
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {

            return (getMipChannel(sector,layer,paddle) >= EXPECTED_MIP_CHANNEL[layer-1]-ALLOWED_MIP_DIFF  &&
                    getMipChannel(sector,layer,paddle) <= EXPECTED_MIP_CHANNEL[layer-1]+ALLOWED_MIP_DIFF);

        }        

    }
    
    private class BANDStatusEventListener extends BANDCalibrationEngine {
        
        public final int[]    EXPECTED_STATUS = {0,0,0};
        public final int  ALLOWED_STATUS_DIFF = 1;
        
        BANDStatusEventListener(){};
        
        public void init(int is1, int is2){
            calib = new CalibrationConstants(3,"stat_left/I:stat_right/I");
            calib.setName("/calibration/band/status");
            calib.setPrecision(3);
            
            for (int i=0 ; i<3; i++) {
                calib.addConstraint(3, EXPECTED_STATUS[i]-ALLOWED_STATUS_DIFF,
                                       EXPECTED_STATUS[i]+ALLOWED_STATUS_DIFF);
            }
/*            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setIntValue(0,"status",is,il,ip);
                    }
                }
            }
            */
            list.add(calib);
        }
    }
           
    public void updateDetectorView(DetectorShape2D shape) {
        BANDCalibrationEngine engine = getSelectedEngine();
        DetectorDescriptor dd = shape.getDescriptor();
        this.getDetIndices(dd);
        layer = lay;
        if (app.omap==3) {
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
