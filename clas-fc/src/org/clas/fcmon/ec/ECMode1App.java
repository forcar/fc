package org.clas.fcmon.ec;


import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
//groot
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;


public class ECMode1App extends FCApplication  {
	
   EmbeddedCanvas c = this.getCanvas(this.getName()); 
    
   public ECMode1App(String name, ECPixels[] ecPix) {
      super(name,ecPix);	
   }
   
   public void updateCanvas(DetectorDescriptor dd) {
		
      int  is = dd.getSector();
      int  la = dd.getLayer();
      int  ic = dd.getComponent();   
      int idet = ilmap;	  
      
      if (la>3) return;
      
      int nstr = ecPix[idet].ec_nstr[la-1];
      int min=0, max=nstr;
      
      switch (ilmap) {
      case 0: c.divide(4,6); max=24 ; if (ic>23) {min=24; max=48;} if (ic>47) {min=48; max=nstr;} break;
      case 1: c.divide(4,3); max=12 ; if (ic>11) {min=12; max=24;} if (ic>23) {min=24; max=nstr;} break;
      case 2: c.divide(4,3); max=12 ; if (ic>11) {min=12; max=24;} if (ic>23) {min=24; max=nstr;}   
      }   
      
      c.setAxisFontSize(14);
		
      int tet = app.mode7Emulation.tet;
      
      if (app.mode7Emulation.User_tet>0)  tet=app.mode7Emulation.User_tet;
      if (app.mode7Emulation.User_tet==0) tet=app.mode7Emulation.CCDB_tet;
      
      F1D f1 = new F1D("p0","[a]",0.,100.); f1.setParameter(0,tet);
      f1.setLineColor(2);
      F1D f2 = new F1D("p0","[a]",0.,100.); f2.setParameter(0,app.mode7Emulation.CCDB_tet);
      f2.setLineColor(4); f2.setLineStyle(2);	
		
      H1F h ;
      String otab[][]={{" U PMT "," V PMT "," W PMT "},
              {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
              {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};
      
      for(int ip=min;ip<max;ip++){
          c.cd(ip-min); 
          c.getPad(ip-min).setOptStat(Integer.parseInt("0"));
          c.getPad(ip-min).getAxisX().setRange(0.,100.);
          c.getPad(ip-min).getAxisY().setRange(-100.,4000*app.displayControl.pixMax);
          h = ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,la,0).sliceY(ip); 
          h.setTitleX("Sector "+is+otab[idet][la-1]+(ip+1)+"  (4 ns/ch)"); h.setTitleY("Counts");
          h.setFillColor(4); c.draw(h);
          h = ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,la,1).sliceY(ip); h.setFillColor(2); c.draw(h,"same");
          c.draw(f1,"same"); c.draw(f2,"same");
          }  

      c.repaint();
   }
}
