package org.clas.fcmon.ctof;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;

public class CTOFMipApp extends FCApplication {
    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
//    F1D f1 = new F1D("landau",300.,3000.);

    public CTOFMipApp(String name, CTOFPixels[] ctofPix) {
        super(name,ctofPix);    
     }

    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd);   
        int  lr = io+1;
        int ilm = ilmap;
        
        int nstr = ctofPix[ilm].nstr;
        int min=0, max=nstr;
    
        c.divide(4,6); max=24 ; if (ic>23) {min=24; max=48;};       
        
        c.setAxisFontSize(12);
//      canvas.setAxisTitleFontSize(12);
//      canvas.setTitleFontSize(14);
//      canvas.setStatBoxFontSize(10);
        
        H1F h;
        String alab;
        String otab[]={" UP PMT "," DN PMT "};
        String lab4[]={" ADC"," TDC","GMEAN PMT "};      

       
        for(int iip=min;iip<max;iip++) {
            alab = otab[lr-1]+(iip+1)+lab4[0];
            c.cd(iip-min);                           
            h = ctofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,lr,0).sliceY(iip); 
            h.setOptStat(Integer.parseInt("110")); 
            h.setTitleX(alab); h.setTitle(""); h.setFillColor(32); c.draw(h);
            h = ctofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(iip);
            h.setFillColor(34); c.draw(h,"same");  
//            if (h.getEntries()>100) {h.fit(f1,"REQ");}
        }

        c.cd(ic-min); 
        h = ctofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(ic); 
        h.setOptStat(Integer.parseInt("110")); 
        alab = "PMT "+(ic+1)+" GMEAN"; h.setTitleX(alab); h.setTitle(""); h.setFillColor(2); c.draw(h); 
        
        c.repaint();

    }
}
