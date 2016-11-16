package org.clas.fcmon.ec;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;

public class ECPixelsApp extends FCApplication {
    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    
    public ECPixelsApp(String name, ECPixels[] ecPix) {
        super(name,ecPix);    
     }
    
    public void analyze() {
        
    }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd);
        c.divide(3, 2);
        
        String tit1[] = {"PCAL Dalitz-2.0","ECinner Dalitz-2.0","ECouter Dalitz-2.0"};
        for (int idet=0; idet<ecPix.length; idet++) {
            c.cd(idet); 
            H1F h1 = ecPix[idet].strips.hmap2.get("H2_PC_Stat").get(is,0,3).sliceY(0);
            H1F h2 = ecPix[idet].strips.hmap2.get("H2_PC_Stat").get(is,0,3).sliceY(1);
            h1.setFillColor(4); h2.setFillColor(32);
            h1.setTitleX(tit1[idet]);
            c.draw(h1);c.draw(h2,"same");
        }   
        
        String tit2[] ={"PCAL Zone 0","PCAL Zone 1","PCAL Zone 2"};
        for (int zone=0; zone<3; zone++) {
            c.cd(zone+3);
            H1F h1 = ecPix[0].strips.hmap2.get("H2_PC_Stat").get(is,0,4).sliceY(zone);
            h1.setFillColor(4); 
            h1.setTitleX(tit2[zone]);
            c.draw(h1);          
        }
        
        c.repaint();

    }    
    
}
