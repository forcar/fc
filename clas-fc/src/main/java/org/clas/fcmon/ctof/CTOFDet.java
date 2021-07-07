package org.clas.fcmon.ctof;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class CTOFDet extends FCDetector {
    
    public CTOFDet(String name , CTOFPixels[] ctofPix) {
        super(name, ctofPix);       
     } 
    
    public void init() {
        initDetector(CTOFConstants.IS1,CTOFConstants.IS2);
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
        
        app.currentView = "UD";
        
        for(int id=0; id<ctofPix.length; id++){
        System.out.println("CTOFDetector.initDetector() is1="+is1+" is2="+is2+" NSTRIPS="+ctofPix[id].nstr);
        for(int is=is1; is<is2; is++) {
            for(int ip=0; ip<ctofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("UD"+id,getPaddle(id,is,id+1,ip,0));
            for(int ip=0; ip<ctofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("UD"+id,getPaddle(id,is,id+1,ip,1));
            for(int ip=0; ip<ctofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("U"+id,getPaddle(id,is,id+1,ip,0));
            for(int ip=0; ip<ctofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("D"+id,getPaddle(id,is,id+1,ip,1));
        }   
        }
        
        app.getDetectorView().getView().addDetectorListener(mon);
        
        for(String layer : app.getDetectorView().getView().getLayerNames()){
            app.getDetectorView().getView().setDetectorListener(layer,mon);
         }
        
        addButtons("DET","View","CTOF.0");
        addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.EPICS.3");
        
        app.getDetectorView().addMapButtons();
        app.getDetectorView().addViewButtons(); 
        
    }    
    
    public DetectorShape2D getPaddle(int det, int sector, int layer, int paddle, int or) {
        
        double rotation = Math.toRadians(360.0/48*(paddle+0.5));
        DetectorShape2D shape = new DetectorShape2D(DetectorType.CTOF,sector,layer,paddle,or);     
        shape.createTrapXY(ctofPix[det].cc.DX2[or], ctofPix[det].cc.DX1[or], ctofPix[det].cc.DY[or]);
        shape.getShapePath().rotateZ(Math.PI/2.);
        shape.getShapePath().translateXYZ(-ctofPix[det].cc.R[or], 0.0, 0.0);
        shape.getShapePath().rotateZ(rotation);

        return shape;       
    }
    
}
