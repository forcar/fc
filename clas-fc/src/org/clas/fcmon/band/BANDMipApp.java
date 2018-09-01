package org.clas.fcmon.band;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;

public class BANDMipApp extends FCApplication {
    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
//    F1D f1 = new F1D("landau",300.,3000.);

    public BANDMipApp(String name, BANDPixels[] bandPix) {
        super(name,bandPix);    
     }

    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd);   
        int  lr = dd.getOrder()+1;
        int ilm = ilmap;
        
        int nstr = bandPix[ilm].nstr[is-1];
        int min=0, max=nstr;
        
        c.clear(); c.divide(2, 4);
        c.setAxisFontSize(12);
        
//      canvas.setAxisTitleFontSize(12);
//      canvas.setTitleFontSize(14);
//      canvas.setStatBoxFontSize(10);
        
        H1F h;
        String alab;
        String otab[]={" L PMT "," R PMT "};
        String lab4[]={" ADC"," TDC","GMEAN PMT "};      
        String tit = "SEC "+is+" LAY "+(ilm+1);
       
        for(int iip=min;iip<max;iip++) {
            alab = tit+otab[lr-1]+(iip+1)+lab4[0];
            c.cd(iip-min);                           
            h = bandPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,lr,0).sliceY(iip); 
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setFillColor(32); c.draw(h);
            h = bandPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(iip);
            h.setFillColor(4); c.draw(h,"same");  
//            if (h.getEntries()>100) {h.fit(f1,"REQ");}
        }

        c.cd(ic-min); 
        h = bandPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(ic); 
        h.setOptStat(Integer.parseInt("1000100")); 
        alab = tit+" BAR "+(ic+1)+" GMEAN"; h.setTitleX(alab); h.setTitle(""); h.setFillColor(2); c.draw(h); 
        
        c.repaint();

    }
}
