package org.clas.fcmon.cc;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;


public class CCPedestalApp extends FCApplication {

    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    
    public CCPedestalApp(String name, CCPixels ccPix) {
        super(name,ccPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        Boolean inMC = (Boolean) mon.getGlob().get("inMC");       
        if (inMC) return;
        
        this.getDetIndices(dd);   
        int lr = layer;
               
        double nstr = ccPix.cc_nstr[0];
        
        H1F h;
        String otab[]={" Left PMT "," Right PMT "};
        
        c.divide(2,2);
        c.setAxisFontSize(14);
        
        for(int il=1;il<3;il++){
            H2F hpix = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,il,3);
            hpix.setTitleX("PED (Ref-Measured)") ; hpix.setTitleY(otab[il-1]);
         
            canvasConfig(c,il-1,-20.,20.,1.,nstr+1,true); c.draw(hpix);
            
            if(lr==il) {
                F1D f1 = new F1D("p0","[a]",-20.,20.); f1.setParameter(0,ic+1);
                F1D f2 = new F1D("p0","[a]",-20.,20.); f2.setParameter(0,ic+2);
                f1.setLineColor(2); c.draw(f1,"same"); 
                f2.setLineColor(2); c.draw(f2,"same");
            }
            
            c.cd(il-1+2);
            h=hpix.sliceY(2);  h.setOptStat(Integer.parseInt("1110")); h.setFillColor(4); h.setTitle(""); 
            h.setTitleX("Sector "+is+otab[il-1]+2); c.draw(h);
            if(lr==il) {h=hpix.sliceY(ic);  h.setOptStat(Integer.parseInt("1110")); h.setFillColor(2); h.setTitle("");
            h.setTitleX("Sector "+is+otab[il-1]+(ic+1)); c.draw(h);}
        }
        
        c.repaint();
    }
    
}
