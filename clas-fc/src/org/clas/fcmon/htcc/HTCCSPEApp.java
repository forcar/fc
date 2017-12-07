package org.clas.fcmon.htcc;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;

public class HTCCSPEApp extends FCApplication {
    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 

    public HTCCSPEApp(String name, HTCCPixels[] ctofPix) {
        super(name,ctofPix);    
     }

    public void updateCanvas(DetectorDescriptor dd) {
        
        H1F h;
        String alab;
        String otab[]={" LEFT PMT "," RIGHT PMT "};
       
        this.getDetIndices(dd);   
        int  lr = layer;
        int ilm = ilmap;
        
        int n=0;
    
        c.divide(4,6);         
        c.setAxisFontSize(12);
//      c.setAxisTitleFontSize(12);
//      c.setTitleFontSize(14);
//      c.setStatBoxFontSize(10);
        
        for(int iis=1; iis<7; iis++) {
        for(int iip=0;iip<4;iip++) {
            alab = "Sector "+iis+" RING "+(iip+1);
            c.cd(n);                           
            h = htccPix[ilm].strips.hmap2.get("H2_a_Hist").get(iis,1,0).sliceY(iip);            
            h.setTitleX(alab); h.setTitle(""); h.setOptStat(Integer.parseInt("11000000")); 
            h.setFillColor(0); c.draw(h);
            h = htccPix[ilm].strips.hmap2.get("H2_a_Hist").get(iis,2,0).sliceY(iip); 
            h.setTitleX(alab); h.setTitle("");  h.setOptStat(Integer.parseInt("11000000")); 
            h.setFillColor(42); c.draw(h,"same");
            
            n++;
        }
        }
        
        c.cd((is-1)*4+ic);
        alab = "Sector "+is+otab[lr-1]+" RING "+(ic+1);
        h = htccPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,lr,0).sliceY(ic);
        h.setTitleX(alab); h.setTitle(""); h.setOptStat(Integer.parseInt("1000100")); 
        h.setFillColor(4); c.draw(h); 

        
        c.repaint();

    }
}
