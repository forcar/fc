package org.clas.fcmon.htcc;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class HTCCDet extends FCDetector {
    
    public HTCCDet(String name , HTCCPixels[] htccPix) {
        super(name, htccPix);       
     } 
    
    public void init() {
        initDetector(HTCCConstants.IS1,HTCCConstants.IS2);
   }
    
    public void initButtons() {
        
        System.out.println("HTCCDetector.initButtons()");
        
        initMapButtons(0, 0);
//        initMapButtons(1, 0);
//        initViewButtons(1, 0);
        initViewButtons(0, 0);
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(1); 
        
    }   
    
    public void initDetector(int is1, int is2) {
        
        app.currentView = "LR";
        
        for(int id=0; id<htccPix.length; id++){
        System.out.println("HTCCDetector.initDetector() is1="+is1+" is2="+is2+" NSTRIPS="+htccPix[id].nstr);
        for(int is=is1; is<is2; is++) {
            for(int ip=0; ip<htccPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,1,ip,0));
            for(int ip=0; ip<htccPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,2,ip,0));
            for(int ip=0; ip<htccPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("L"+id,getPaddle(id,is,2,ip,0));
            for(int ip=0; ip<htccPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("R"+id,getPaddle(id,is,2,ip,0));
        }   
        }
        
        app.getDetectorView().getView().addDetectorListener(mon);
        
        for(String layer : app.getDetectorView().getView().getLayerNames()){
            app.getDetectorView().getView().setDetectorListener(layer,mon);
         }
        
        addButtons("DET","View","HTCC.0");
        addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.EPICS.3");
        
        app.getDetectorView().addMapButtons();
        app.getDetectorView().addViewButtons(); 
        
    }    
    
    public DetectorShape2D getPaddle(int det, int sec, int lay, int pmt, int ord) {
        
        double rotation = Math.toRadians(360.0/htccPix[det].htcc_nsec[0]*(sec-1));
        
        DetectorShape2D shape = new DetectorShape2D(DetectorType.HTCC,sec,lay,pmt,ord);     
        shape.createSplitTrapXY(lay-1,htccPix[det].cc.UB[pmt], htccPix[det].cc.LB[pmt], htccPix[det].cc.THICK);
        shape.getShapePath().rotateZ(Math.PI/2.);
        shape.getShapePath().translateXYZ(-htccPix[det].cc.R[pmt], 0.0, 0.0);       
        shape.getShapePath().rotateZ(rotation);

        return shape;       
    }
    
}
