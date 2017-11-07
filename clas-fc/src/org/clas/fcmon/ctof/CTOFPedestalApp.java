package org.clas.fcmon.ctof;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
//import org.root.histogram.H2D;
//groot
//groot
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class CTOFPedestalApp extends FCApplication {

    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    int ics=1;
    
    public CTOFPedestalApp(String name, CTOFPixels[] ctofPix) {
        super(name,ctofPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd);   
        int  lr = dd.getOrder()+1;
        int ilm = ilmap;
        
        int col2=2,col4=4,col0=0;
        double nstr = ctofPix[ilm].nstr;
        
        H1F h;
        String otab[]={" UP PMT "," DN PMT "};
        
        c.divide(2,2);
        c.setAxisFontSize(14);
//        canvas.setTitleFontSize(14);
//        canvas.setAxisTitleFontSize(14);
        
        for(int il=1;il<3;il++){
            H2F hpix = ctofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,il,3);
            hpix.setTitleX("PED (Ref-Measured)") ; hpix.setTitleY(otab[il-1]);         
            canvasConfig(c,il-1,-20.,20.,1.,nstr+1,true) ; c.draw(hpix);
            if(lr==il) {
                F1D f1 = new F1D("p0","[a]",-20.,20.); f1.setParameter(0,ic+1);
                F1D f2 = new F1D("p0","[a]",-20.,20.); f2.setParameter(0,ic+2);
                f1.setLineColor(2); c.draw(f1,"same"); 
                f2.setLineColor(2); c.draw(f2,"same");
            }
            
            c.cd(il-1+2);
            h=hpix.sliceY(ics); h.setOptStat(Integer.parseInt("110")); h.setFillColor(4); h.setTitle(""); h.setTitleX("Sector "+is+otab[il-1]+(ics+1)); c.draw(h);
            if(lr==il) {h=hpix.sliceY(ic) ; h.setFillColor(2); h.setTitle(" "); h.setTitleX("Sector "+is+otab[il-1]+(ic+1));  c.draw(h);}
            
        }  
        
        c.repaint();
        ics = ic;
    }
    
}
