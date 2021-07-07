package org.clas.fcmon.band;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class BANDDet extends FCDetector {
    
    public BANDDet(String name , BANDPixels[] bandPix) {
        super(name, bandPix);       
     } 
    
    public void init() {
        initDetector(BANDConstants.IS1,BANDConstants.IS2);
   }
    
    public void initButtons() {
        
        System.out.println("BANDDetector.initButtons()");
        
        initMapButtons(0, 0);
        initMapButtons(1, 0);
        initViewButtons(1, 0);
        initViewButtons(0, 0);
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(1); 
        
    }   
    
    public void initDetector(int is1, int is2) {
        
        app.currentView = "LR";
               
        for(int id=0; id<bandPix.length; id++){
        for(int is=is1; is<is2; is++) {
            System.out.println("BANDDetector.initDetector() is1="+is1+" is2="+is2+"NBARS="+bandPix[id].nstr[is-1]);
            for(int ip=0; ip<bandPix[id].nstr[is-1] ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,id+1,ip,0));
            for(int ip=0; ip<bandPix[id].nstr[is-1] ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,id+1,ip,1));
            for(int ip=0; ip<bandPix[id].nstr[is-1] ; ip++) app.getDetectorView().getView().addShape("L"+id,getPaddle(id,is,id+1,ip,0));
            for(int ip=0; ip<bandPix[id].nstr[is-1] ; ip++) app.getDetectorView().getView().addShape("R"+id,getPaddle(id,is,id+1,ip,1));
        }   
        }
        
//        app.getDetectorView().getView().addShape("LR"+0,getPMT(0,is,1,0,0));
        
        app.getDetectorView().getView().addDetectorListener(mon);
        
        for(String layer : app.getDetectorView().getView().getLayerNames()){
            app.getDetectorView().getView().setDetectorListener(layer,mon);
         }
        
        addButtons("DET","View","L1.0.L2.1.L3.2.L4.3.L5.4");
        addButtons("LAY","View","LR.0.L.1.R.2");
        addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
        addButtons("PIX","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
        
        app.getDetectorView().addMapButtons();
        app.getDetectorView().addViewButtons(); 
        
    }    
    
    public DetectorShape2D getPMT(int det, int sector, int layer, int paddle, int order) {
        DetectorShape2D shape = new DetectorShape2D(DetectorType.BAND,sector,layer,paddle,order);     
        shape.createCirc(0., 0., 10.);
        return shape;
    }
    
    public DetectorShape2D getPaddle(int det, int sector, int layer, int paddle, int order) {
        
        DetectorShape2D shape = new DetectorShape2D(DetectorType.BAND,sector,layer,paddle,order);     
        Path3D shapePath = shape.getShapePath();
        
//        int off = (layer-1)*ftofPix[det].nstr;
        int off = order*bandPix[det].nstr[sector-1];
        
        for(int j = 0; j < 4; j++){
            shapePath.addPoint(-bandPix[det].band_xpix[j][paddle+off][sector-1],-bandPix[det].band_ypix[j][paddle+off][sector-1],0.0);
        }
        return shape;       
    }
    
}
