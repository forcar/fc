package org.clas.fcmon.cc;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class CCDetector extends FCDetector {
    
    public CCDetector(String name , CCPixels ccPix) {
        super(name, ccPix);       
     } 
    
    public void init(int is1, int is2) {
        initDetector(is1,is2);
   }
    
    public void initButtons() {
        
        System.out.println("CCDetector.initButtons()");
        
        initMapButtons(0, 0);
        initMapButtons(1, 0);
        initViewButtons(0, 0); 
        
    }   
    
    public void initDetector(int is1, int is2) {
        
        app.currentView = "LR";
        
        for(int is=is1; is<is2; is++) {
            for(int ip=0; ip<ccPix.cc_nstr[0] ; ip++) app.getDetectorView().getView().addShape("LR0",getMirror(is,1,ip));
            for(int ip=0; ip<ccPix.cc_nstr[1] ; ip++) app.getDetectorView().getView().addShape("LR0",getMirror(is,2,ip));
            for(int ip=0; ip<ccPix.cc_nstr[0] ; ip++) app.getDetectorView().getView().addShape("L0",getMirror(is,1,ip));
            for(int ip=0; ip<ccPix.cc_nstr[1] ; ip++) app.getDetectorView().getView().addShape("R0",getMirror(is,2,ip));
        }   
        
        app.getDetectorView().getView().addDetectorListener(mon);
        
        for(String layer : app.getDetectorView().getView().getLayerNames()){
            app.getDetectorView().getView().setDetectorListener(layer,mon);
         }
        
        addButtons("LAY","View","LR.0.L.1.R.2");
        addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
        addButtons("PIX","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
        
        app.getDetectorView().addMapButtons();
        app.getDetectorView().addViewButtons(); 
        
    }    
    
    public DetectorShape2D getMirror(int sector, int layer, int mirror) {
        
        DetectorShape2D shape = new DetectorShape2D(DetectorType.LTCC,sector,layer,mirror);     
        Path3D shapePath = shape.getShapePath();
        
        int off = (layer-1)*ccPix.cc_nstr[0];
        
        for(int j = 0; j < 4; j++){
            shapePath.addPoint(ccPix.cc_xpix[j][mirror+off][sector-1],ccPix.cc_ypix[j][mirror+off][sector-1],0.0);
        }
        return shape;       
    }
    
}
