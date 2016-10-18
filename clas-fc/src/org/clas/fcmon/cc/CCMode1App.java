package org.clas.fcmon.cc;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;

public class CCMode1App extends FCApplication {
    
    public CCMode1App(String name, CCPixels ccPix) {
        super(name,ccPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        EmbeddedCanvas c = this.getCanvas(this.getName()); 
        this.getDetIndices(dd);   
        int lr = layer;
        
        c.divide(3,6);
        c.setAxisFontSize(14);
        
        H1F h = new H1F() ; 
        String otab[]={" Left PMT "," Right PMT "};
        
        int tet = app.mode7Emulation.tet;
        
        if (app.mode7Emulation.User_tet>0)  tet=app.mode7Emulation.User_tet;
        if (app.mode7Emulation.User_tet==0) tet=app.mode7Emulation.CCDB_tet;
        
        F1D f1 = new F1D("p0","[a]",0.,100.); f1.setParameter(0,tet);
        f1.setLineColor(2);
        F1D f2 = new F1D("p0","[a]",0.,100.); f2.setParameter(0,app.mode7Emulation.CCDB_tet);
        f2.setLineColor(4);f2.setLineStyle(2);
       
        for(int ip=0;ip<ccPix.cc_nstr[lr-1];ip++){
            c.cd(ip); c.getPad(ip).setAxisRange(0.,100.,-15.,4000*app.displayControl.pixMax);
            h = ccPix.strips.hmap2.get("H2_CCa_Sevd").get(is,lr,0).sliceY(ip); h.setTitleX("Samples (4 ns)"); h.setTitleY("Counts");
            h.setTitle("Sector "+is+otab[lr-1]+(ip+1)); h.setFillColor(4); c.draw(h);
            h = ccPix.strips.hmap2.get("H2_CCa_Sevd").get(is,lr,1).sliceY(ip); h.setFillColor(2); c.draw(h,"same");
            c.draw(f1,"same");c.draw(f2,"same");
            }  
        c.repaint();
    }   
    
}
