package org.clas.fcmon.tools;

import java.util.TreeMap;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
//import org.root.histogram.H1D;
//import org.root.histogram.H2D;

public class Pixel {
    
    public int index;
    public DetectorShape2D shape;  
    public double area;
    public int zone;
    public int u;
    public int v;
    public int w;
    public int[] strips = new int[3];
    public double udist;
    public double vdist;
    public double wdist;
    public TreeMap<String, H1F>   h1d = null; 
    public TreeMap<String, H2F>   h2d = null; 
    public boolean status;
    
    double[] theta={0.0,60.0,120.0,180.0,240.0,300.0};

    public void setIndex(int index) {
        this.index = index;
    }
    
    public void setShape(DetectorShape2D shape){
        this.shape = shape;
     }
    
    public void setArea(double area){
        this.area = area;
    }
    
    public void setZone(int u, int v, int w){
        if (u<53&&v>15&&w>15) this.zone=0;
        if (u>52&&v>15&&w>15) this.zone=1;
        if (v<16)             this.zone=2;
        if (w<16)             this.zone=3;
    }
    
    public void setReadout(int u, int v, int w) {
        this.u=u;
        this.v=v;
        this.w=w;
        this.strips[0]=u;
        this.strips[1]=v;
        this.strips[2]=w;
        setZone(u,v,w);
    }
    
    public void setReadoutDist(double udist, double vdist, double wdist) {
        this.udist=udist;
        this.vdist=vdist;
        this.wdist=wdist;   
    } 
    
    public void addH1DMap(String name, H1F h1d) {
        this.h1d.put(name,h1d);
    }
    
    public void addH2DMap(String name, H2F h2d) {
        this.h2d.put(name,h2d);
    }  
    
    public void setStatus(boolean status) {
        this.status=status;
    }   
    
    public DetectorShape2D rotatePixel(int sector, int pixel) {
        DetectorShape2D rotshape = this.shape;
        double thet=theta[sector]*3.14159/180.;
        double ct=Math.cos(thet) ; double st=Math.sin(thet);
        for(int i = 0; i < this.shape.getShapePath().size(); ++i) {
            double x = this.shape.getShapePath().point(i).x();
            double y = this.shape.getShapePath().point(i).y();
            double xrot = -(x*ct+y*st);
            double yrot =  -x*st+y*ct;
            rotshape.getShapePath().point(i).set(xrot, yrot, 0.0);
        }
        return rotshape;
    }
    
    public double getArea() {
        return area;
    }
    
    public int getZone() {
        return zone;
    }
    
}
