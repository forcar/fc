package org.clas.fcmon.cc;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class CCDet extends FCDetector {
    
    public CCDet(String name , CCPixels ccPix) {
        super(name, ccPix);       
     } 
    
    public void init() {
        initDetector(CCConstants.IS1,CCConstants.IS2);
   }
    
    public void initButtons() {
        
        System.out.println("CCDetector.initButtons()");
        
        initMapButtons(0, 0);
        initMapButtons(1, 0);
        initViewButtons(0, 0); 
        
    }   
    
    public void initDetector(int is1, int is2) {
        
        app.currentView = "LRR";
        
        System.out.println("CCDetector.initDetector() is1="+is1+" is2="+is2+" NSTRIPS="+ccPix.nstr);        
        for(int is=is1; is<is2; is++) {
            for(int ip=0; ip<ccPix.cc_nstr[0] ; ip++) app.getDetectorView().getView().addShape("LR0",getMirror(is,1,ip,0));
            for(int ip=0; ip<ccPix.cc_nstr[0] ; ip++) app.getDetectorView().getView().addShape("LR0",getMirror(is,1,ip,1));
            for(int ip=0; ip<ccPix.cc_nstr[0] ; ip++) app.getDetectorView().getView().addShape("L0",getMirror(is,1,ip,0));
            for(int ip=0; ip<ccPix.cc_nstr[0] ; ip++) app.getDetectorView().getView().addShape("R0",getMirror(is,1,ip,1));
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
    
    public DetectorShape2D getMirror(int sector, int layer, int mirror, int or) {
        
        DetectorShape2D shape = new DetectorShape2D(DetectorType.LTCC,sector,layer,mirror,or);     
        Path3D shapePath = shape.getShapePath();
        
        int off = (layer-1)*ccPix.cc_nstr[0];
        
        for(int j = 0; j < 4; j++){
            shapePath.addPoint(ccPix.cc_xpix[j][mirror+off][sector-1],ccPix.cc_ypix[j][mirror+off][sector-1],0.0);
        }
        return shape;       
    }
    
}
