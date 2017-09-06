package org.clas.fcmon.ec;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class ECDet extends FCDetector {
    
    DetectorType[] detNames = {DetectorType.ECAL, DetectorType.ECIN, DetectorType.ECOUT};
    
    public ECDet(String name , ECPixels[] ecPix) {
        super(name, ecPix);       
     }    
    
    public void init() {
        initDetector(ECConstants.IS1,ECConstants.IS2);
    }  
  
    public void initButtons() {
        
        System.out.println("ECDetector.initButtons()");
        
        initMapButtons(0, 0);
        initMapButtons(1, 0);
        initViewButtons(0, 0);
        initViewButtons(1, 3);        
    }
    
    public void initDetector(int is1, int is2) {
        
        app.currentView = "PIX";
        
        for(int id=0; id<ecPix.length; id++){
        System.out.println("ECDetector.initDetector() is1="+ECConstants.IS1+" is2="+ECConstants.IS2+" NSTRIPS="+ecPix[id].ec_nstr[0]);
        for(int is=is1; is<is2; is++) {      
            for(int ip=0; ip<ecPix[id].ec_nstr[0] ; ip++)             app.getDetectorView().getView().addShape("U"+id,getStrip(id,is,1,ip));
            for(int ip=0; ip<ecPix[id].ec_nstr[1] ; ip++)             app.getDetectorView().getView().addShape("V"+id,getStrip(id,is,2,ip));
            for(int ip=0; ip<ecPix[id].ec_nstr[2] ; ip++)             app.getDetectorView().getView().addShape("W"+id,getStrip(id,is,3,ip));           
            for(int ip=0; ip<ecPix[id].pixels.getNumPixels() ; ip++)  app.getDetectorView().getView().addShape("PIX"+id,getPixel(id,is,4,ip));
        }
        }
        
         app.getDetectorView().getView().addDetectorListener(mon);
         
         for(String layer : app.getDetectorView().getView().getLayerNames()){
            app.getDetectorView().getView().setDetectorListener(layer,mon);
         }
         
         addButtons("DET","View","PC.0.ECi.1.ECo.2");
         addButtons("LAY","View","U.0.V.1.W.2.PIX.3");
         addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
         addButtons("PIX","Map","EVT.0.NEVT.1.U PIX.11.V PIX.12.W PIX.13.U+V+W.9");
     
         app.getDetectorView().addMapButtons();
         app.getDetectorView().addViewButtons();
         
    }
    
    public DetectorShape2D getPixel(int det, int sector, int layer, int pixel){

        DetectorShape2D shape = new DetectorShape2D(detNames[det],sector,layer,pixel);               
        Path3D      shapePath = shape.getShapePath();
        
        for(int j = 0; j < ecPix[det].ec_nvrt[pixel]; j++){
            shapePath.addPoint(ecPix[det].ec_xpix[j][pixel][sector-1],ecPix[det].ec_ypix[j][pixel][sector-1],0.0);
        }
        
        shape.addDetectorListener(mon);
        return shape;
    }
    
    public DetectorShape2D getStrip(int det, int sector, int layer, int str) {

        DetectorShape2D shape = new DetectorShape2D(detNames[det],sector,layer,str);               
        Path3D      shapePath = shape.getShapePath();
        
        for(int j = 0; j <4; j++){
            shapePath.addPoint(ecPix[det].ec_xstr[j][str][layer-1][sector-1],ecPix[det].ec_ystr[j][str][layer-1][sector-1],0.0);
        }   
        
        shape.addDetectorListener(mon);
        return shape;
    }
        
}
