package org.clas.fcmon.cnd;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class CNDDet extends FCDetector {
    
    public CNDDet(String name , CNDPixels[] cndPix) {
        super(name, cndPix);       
     } 
    
    public void init() {
        initDetector(CNDConstants.IS1,CNDConstants.IS2);
   }
    
    public void initButtons() {
        
        System.out.println("CTOFDetector.initButtons()");
        
        initMapButtons(0, 0);
//        initMapButtons(1, 0);
//        initViewButtons(1, 0);
        initViewButtons(0, 0);
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(1); 
        
    }   
    
    public void initDetector(int is1, int is2) {
        
        app.currentView = "LR";
        
        for(int id=0; id<cndPix.length; id++){
        System.out.println("CNDDetector.initDetector() is1="+is1+" is2="+is2+" NSTRIPS="+cndPix[id].nstr);
        for(int is=is1; is<is2; is++) {
            for(int ip=0; ip<cndPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,id+1,ip,0));
            for(int ip=0; ip<cndPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,id+1,ip,1));
        }   
        }
        
        app.getDetectorView().getView().addDetectorListener(mon);
        
        for(String layer : app.getDetectorView().getView().getLayerNames()){
            app.getDetectorView().getView().setDetectorListener(layer,mon);
         }
        
        addButtons("DET","View","CND.0");
        addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
        
        app.getDetectorView().addMapButtons();
        app.getDetectorView().addViewButtons(); 
        
    }    
    
    public DetectorShape2D getPaddle(int det, int sector, int layer, int pmt, int order) {
        
        double rotation = Math.toRadians(360.0/cndPix[det].cnd_nsec[0]*(sector-1));
        
        DetectorShape2D shape = new DetectorShape2D(DetectorType.CND,sector,layer,pmt,order); 
        shape.createSplitTrapXY(order,cndPix[det].cc.UB[pmt], cndPix[det].cc.LB[pmt], cndPix[det].cc.THICK);
        shape.getShapePath().rotateZ(3.14159/2.);
        shape.getShapePath().translateXYZ(-cndPix[det].cc.R[pmt], 0.0, 0.0);       
        shape.getShapePath().rotateZ(rotation);

        return shape;       
    }
    
}
