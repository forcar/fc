package org.clas.fcmon.ftof;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.FCDetector;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class FTOFDet extends FCDetector {
    
    public FTOFDet(String name , FTOFPixels[] ftofPix) {
        super(name, ftofPix);       
     } 
    
    public void init() {
        initDetector(FTOFConstants.IS1,FTOFConstants.IS2);
   }
    
    public void initButtons() {
        
        System.out.println("FTOFDetector.initButtons()");
        
        initMapButtons(0, 0);
        initMapButtons(1, 0);
        initViewButtons(1, 0);
        initViewButtons(0, 0);
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(1); 
        
    }   
    
    public void initDetector(int is1, int is2) {
        
        app.currentView = "LR";
               
        for(int id=0; id<ftofPix.length; id++){
        System.out.println("FTOFDetector.initDetector() is1="+is1+" is2="+is2+"NSTRIPS="+ftofPix[id].nstr);
        for(int is=is1; is<is2; is++) {
            for(int ip=0; ip<ftofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,id+1,ip,0));
            for(int ip=0; ip<ftofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("LR"+id,getPaddle(id,is,id+1,ip,1));
            for(int ip=0; ip<ftofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("L"+id,getPaddle(id,is,id+1,ip,0));
            for(int ip=0; ip<ftofPix[id].nstr ; ip++) app.getDetectorView().getView().addShape("R"+id,getPaddle(id,is,id+1,ip,1));
        }   
        }
        
        app.getDetectorView().getView().addDetectorListener(mon);
        
        for(String layer : app.getDetectorView().getView().getLayerNames()){
            app.getDetectorView().getView().setDetectorListener(layer,mon);
         }
        
        addButtons("DET","View","PANEL1A.0.PANEL1B.1.PANEL2.2");
        addButtons("LAY","View","LR.0.L.1.R.2");
        addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
        addButtons("PIX","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
        
        app.getDetectorView().addMapButtons();
        app.getDetectorView().addViewButtons(); 
        
    }    
    
    public DetectorShape2D getPaddle(int det, int sector, int layer, int paddle, int order) {
        
        DetectorShape2D shape = new DetectorShape2D(DetectorType.FTOF,sector,layer,paddle,order);     
        Path3D shapePath = shape.getShapePath();
        
//        int off = (layer-1)*ftofPix[det].nstr;
        int off = order*ftofPix[det].nstr;
        
        for(int j = 0; j < 4; j++){
            shapePath.addPoint(ftofPix[det].ftof_xpix[j][paddle+off][sector-1],ftofPix[det].ftof_ypix[j][paddle+off][sector-1],0.0);
        }
        return shape;       
    }
    
}
