package org.clas.fcmon.ec;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;

import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class ECOccupancyApp extends FCApplication {
	
   public ECOccupancyApp(String name, ECPixels[] ecPix) {
      super(name,ecPix);		
   }
		
   public void updateCanvas(DetectorDescriptor dd) {
		
      EmbeddedCanvas canvas = this.getCanvas(this.getName()); 
      
      H1F h;
      String lab1[]={"U ","V ","W "}, lab2[]={"Inner ","Outer "}, lab3[]={"Strip ","Pixel "},lab4[]={" ADC"," TDC"};
        
      this.getDetIndices(dd);
      
      layer = lay ; 
      
      int l,col0=0,col1=0,col2=0,strip=0,pixel=0;

      canvas.divide(3,3);
      canvas.setAxisFontSize(14);
      canvas.setStatBoxFontSize(12);
	    		
      if (layer<7)  {col0=0 ; col1=4; col2=2;strip=ic+1;}
      if (layer>=7) {col0=4 ; col1=4; col2=2;pixel=ic+1;}
		
      DetectorCollection<H2F> dc2a = ecPix[0].strips.hmap2.get("H2_a_Hist");
      DetectorCollection<H2F> dc2t = ecPix[0].strips.hmap2.get("H2_t_Hist");
  
      for(int il=l1;il<l2;il++){
         String otab = lab1[il-1-of]+lab2[io-1]+"Strips";
         canvas.cd(il-1-of); h = dc2a.get(is+1,il,0).projectionY(); h.setTitleX(otab); h.setFillColor(col0); canvas.draw(h);
      }
		
      l=layer;
		
      if (layer<7) {
         canvas.cd(l-1-of); h = dc2a.get(is+1,l,0).projectionY(); h.setFillColor(col1); canvas.draw(h,"same");
         H1F copy = h.histClone("Copy"); copy.reset() ; 
         copy.setBinContent(ic, h.getBinContent(ic)); copy.setFillColor(col2); canvas.draw(copy,"same");
         for(int il=l1;il<l2;il++) {
            String alab = lab1[il-1-of]+lab2[io-1]+lab3[0]+strip+lab4[0];String tlab = lab1[il-1-of]+lab2[io-1]+lab3[0]+strip+lab4[1];
            if(layer!=il) {canvas.cd(il+2-of); h = dc2a.get(is+1,il,0).sliceY(22); h.setTitleX(alab); h.setTitle(""); h.setFillColor(col0); canvas.draw(h);}
            if(layer!=il) {canvas.cd(il+5-of); h = dc2t.get(is+1,il,0).sliceY(22); h.setTitleX(tlab); h.setTitle(""); h.setFillColor(col0); canvas.draw(h);}
         }
         String alab = lab1[l-1-of]+lab2[io-1]+lab3[0]+strip+lab4[0];String tlab = lab1[l-1-of]+lab2[io-1]+lab3[0]+strip+lab4[1];
         canvas.cd(l+2-of); h = dc2a.get(is+1,l,0).sliceY(ic);h.setTitleX(alab); h.setTitle(""); h.setFillColor(col2); canvas.draw(h,"S");
         canvas.cd(l+5-of); h = dc2t.get(is+1,l,0).sliceY(ic);h.setTitleX(tlab); h.setTitle(""); h.setFillColor(col2); canvas.draw(h,"S");
      }
		
      if (layer==7||layer==8) {
         for(int il=l1;il<l2;il++) {
            canvas.cd(il-1-of); h = dc2a.get(is+1,il,0).projectionY();
            H1F copy = h.histClone("Copy");			
            strip = ecPix[0].pixels.getStrip(il-of,ic+1);
            copy.reset() ; copy.setBinContent(ic, h.getBinContent(ic));
            copy.setFillColor(col2); canvas.draw(copy,"same");	    		 
            String alab = lab1[il-1-of]+lab2[io-1]+lab3[0]+strip+lab4[0];String tlab = lab1[il-1-of]+lab2[io-1]+lab3[0]+strip+lab4[1];
            canvas.cd(il+2-of) ; h = dc2a.get(is+1,il,0).sliceY(strip-1); h.setTitleX(alab); h.setTitle("");h.setFillColor(col2); canvas.draw(h,"S");
            canvas.cd(il+5-of) ; h = dc2t.get(is+1,il,0).sliceY(strip-1); h.setTitleX(tlab); h.setTitle("");h.setFillColor(col2); canvas.draw(h,"S");
         }
      }
		
      if (layer>8) {
         for(int il=l1;il<l2;il++) {
            canvas.cd(il-1-of); h = dc2a.get(is+1,il,1).projectionY();
            H1F copy = h.histClone("Copy");
            strip = ecPix[0].pixels.getStrip(il-of,ic+1);
            copy.reset() ; copy.setBinContent(strip-1, h.getBinContent(strip-1));
            copy.setFillColor(col2); canvas.draw(copy,"same");	
            String alab1 = lab1[il-1-of]+lab2[io-1]+lab3[0]+strip+lab4[0];String tlab1 = lab1[il-1-of]+lab2[io-1]+lab3[0]+strip+lab4[1];
            String alab2 = lab1[il-1-of]+lab2[io-1]+lab3[1]+pixel+lab4[0];String tlab2 = lab1[il-1-of]+lab2[io-1]+lab3[1]+pixel+lab4[1];
            if (layer<17) {
               canvas.cd(il+2-of) ; h = dc2a.get(is+1,il,1).sliceY(strip-1); h.setTitleX(alab1); h.setTitle("");h.setFillColor(col2); canvas.draw(h,"S");
               canvas.cd(il+5-of) ; h = dc2a.get(is+1,il,2).sliceY(ic)     ; h.setTitleX(alab2); h.setTitle("");h.setFillColor(col2); canvas.draw(h,"S");
            }
            if (layer>16&&layer<22) {
               canvas.cd(il+2-of) ; h = dc2t.get(is+1,il,1).sliceY(strip-1); h.setTitleX(tlab1); h.setTitle("");h.setFillColor(col2); canvas.draw(h);
               canvas.cd(il+5-of) ; h = dc2t.get(is+1,il,2).sliceY(ic)     ; h.setTitleX(tlab2); h.setTitle("");h.setFillColor(col2); canvas.draw(h);
            }
	     }  	 
	  }
      
   }
}
