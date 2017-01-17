package org.clas.fcmon.cc;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;

public class CCMode1App extends FCApplication {
    
    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    mode1  = new EmbeddedCanvasTabbed("Event");
    EmbeddedCanvas               c = this.getCanvas(this.getName()); 
    
    int is,lr,ic;
    int ics[] = new int[2];
    String otab[]={" Left PMT "," Right PMT "};
    
    public CCMode1App(String name, CCPixels ccPix) {
        super(name,ccPix);    
     }
    
    public JPanel getPanel() {
        engineView.setLayout(new BorderLayout());
        mode1.addCanvas("Sum");
        engineView.add(mode1);
        return engineView;
    } 
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.is = dd.getSector();
        this.lr = dd.getLayer();
        this.ic = dd.getComponent();   
        
        if (lr>3) return;
        
        switch (mode1.selectedCanvas) {
        case "Event": updateEvent(); break;
        case   "Sum": updateSum();
        }  
        
    }
    
    public void updateEvent() {

        int min=0;
        c = mode1.getCanvas("Event");       
    
        c.divide(3,6);
        c.setAxisFontSize(14);
        
        int tet = app.mode7Emulation.tet;
        
        if (app.mode7Emulation.User_tet>0)  tet=app.mode7Emulation.User_tet;
        if (app.mode7Emulation.User_tet==0) tet=app.mode7Emulation.CCDB_tet;
        
        F1D f1 = new F1D("p0","[a]",0.,100.); f1.setParameter(0,tet);
        f1.setLineColor(2);
        F1D f2 = new F1D("p0","[a]",0.,100.); f2.setParameter(0,app.mode7Emulation.CCDB_tet);
        f2.setLineColor(4);f2.setLineStyle(2);
        
        H1F h ; 
              
        c.clear();
        
        for(int ip=0;ip<ccPix.cc_nstr[lr-1];ip++){
            c.cd(ip); c.getPad(ip).setOptStat(Integer.parseInt("0")); 
            c.getPad(ip).setAxisRange(0.,100.,-15.,4000*app.displayControl.pixMax);
            h = ccPix.strips.hmap2.get("H2_CCa_Sevd").get(is,lr,0).sliceY(ip); 
            h.setTitleX("Sector "+is+otab[lr-1]+(ip+1)+" (4 ns/ch)"); h.setTitleY("Counts");
            h.setFillColor(4); c.draw(h);
            h = ccPix.strips.hmap2.get("H2_CCa_Sevd").get(is,lr,1).sliceY(ip); 
            h.setFillColor(2); c.draw(h,"same");
            c.draw(f1,"same"); c.draw(f2,"same");
            }  
        c.repaint();
    }   
    
    public void updateSum() {
        
        DetectorCollection<H2F> dc2a = ccPix.strips.hmap2.get("H2_CCa_Hist");        
        H1F h1; H2F h2;
        
        F1D f1 = new F1D("p0","[a]",0.,100.); 
        F1D f2 = new F1D("p0","[a]",0.,100.); 
        f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(2);
        f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(2);
               
        c = mode1.getCanvas("Sum");  c.clear(); c.divide(2,2);       
            
        for (int il=1; il<3 ; il++) {
            int nstr = ccPix.cc_nstr[il-1];
            h2 = dc2a.get(is,il,5); h2.setTitleY("Sector "+is+otab[il-1]) ; h2.setTitleX("SAMPLES (4 ns/ch)");
            canvasConfig(c,il-1,0.,100.,1.,nstr+1.,true).draw(h2);
            if (lr==il) {c.draw(f1,"same"); c.draw(f2,"same");}
            h1 = dc2a.get(is,il,5).sliceY(ics[il-1]); h1.setOptStat(Integer.parseInt("10"));
            h1.setTitleX("Sector "+is+otab[il-1]+(ics[il-1]+1)+" (4 ns/ch)"); h1.setFillColor(0);
            c.cd(il+1); h1.setTitle(" "); c.draw(h1);
            if (lr==il) {h1=dc2a.get(is,il,5).sliceY(ic) ; h1.setFillColor(2); h1.setOptStat(Integer.parseInt("10"));
            h1.setTitleX("Sector "+is+otab[il-1]+(ic+1)+" (4 ns/ch)"); c.draw(h1);}
        }
        
        c.repaint();
        ics[lr-1]=ic;
        
     }     
}
