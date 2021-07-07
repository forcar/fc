package org.clas.fcmon.tools;

// Handles collections of pixel objects and associated H1D,H2D collections

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.jlab.detector.base.DetectorCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
//import org.root.histogram.H1F;
//import org.root.histogram.H2F;

public class Pixels {

    FTHashCollection<Integer>       pixelDummy = null;
    public FTHashCollection<Pixel> pixelStrips = null;
    FTHashCollection<Pixel>        pixelNumber = null;
    
    DetectorCollection<List<H1F>>       pixelH1D = null;
    DetectorCollection<List<H2F>>       pixelH2D = null;
    
    public TreeMap<String, DetectorCollection<H1F>>   hmap1 = null; 
    public TreeMap<String, DetectorCollection<H2F>>   hmap2 = null; 
    
    public double[] maxZonePixelArea = {0,0,0,0};
    double              maxPixelArea = 0;
    public Pixels() {
        this.pixelStrips = new FTHashCollection<Pixel>(3);
        this.pixelNumber = new FTHashCollection<Pixel>(1);
        this.pixelH1D    = new DetectorCollection<List<H1F>>();
        this.pixelH2D    = new DetectorCollection<List<H2F>>();
        this.hmap1       = new TreeMap<String, DetectorCollection<H1F>>();
        this.hmap2       = new TreeMap<String, DetectorCollection<H2F>>();
    }
    
    public void addPixel(int pix, int u, int v, int w) {
        pixelDummy.add(pix,u,v,w); 
    }   
    
    public void addPixel(Pixel pixel, int pix, int u, int v, int w) {
        pixelStrips.add(pixel,u,v,w); 
        pixelNumber.add(pixel, pix);
        findMaxPixelArea(pixel);
    }
    
    public void addH1D(int sector, int layer, int component, H1F h1) {
        if(!pixelH1D.hasEntry(sector,layer,component)) pixelH1D.add(sector,layer,component,new ArrayList<H1F>());
        pixelH1D.get(sector,layer,component).add(h1);       
    }
    
    public void addH2D(int sector, int layer, int component, H2F h2) {
        if(!pixelH2D.hasEntry(sector,layer,component)) pixelH2D.add(sector,layer,component,new ArrayList<H2F>());
        pixelH2D.get(sector,layer,component).add(h2);       
    }    
    
    public void addH1DMap(String name, DetectorCollection<H1F> map) {
        this.hmap1.put(name,map);
    }
    
    public void addH2DMap(String name, DetectorCollection<H2F> map) {
        this.hmap2.put(name,map);
    }   
    
    public void setMaxPixelArea(int zone, double area) {
    	this.maxZonePixelArea[zone] = area;
    }
    
    public Pixel getPixel(int u, int v, int w) {
        Pixel pixel=null;
        if (pixelStrips.hasItem(u,v,w)) pixel = pixelStrips.getItem(u,v,w);
        return pixel;    
    }
    
    public Pixel getPixel(int index) {
        Pixel pixel=null;
        if (pixelNumber.hasItem(index)) pixel = pixelNumber.getItem(index);
        return pixel;    
    }  
    
    public int getPixelNumber(int u, int v, int w) {
        int pixel=0;
        if (pixelStrips.hasItem(u,v,w)) pixel = pixelStrips.getItem(u,v,w).index;
        return pixel;
    }
    
    public boolean getPixelStatus(int pixel) {
    	return getPixel(pixel).status;
    }
    
    public double getUdist(int pixel) {
    	return getPixel(pixel).udist;
    }
    
    public double getVdist(int pixel) {
    	return getPixel(pixel).vdist;
    }
    
    public double getWdist(int pixel) {
    	return getPixel(pixel).wdist;
    }
    
    public double[] getDist(int layer) {
    	double[] dist = new double[this.getNumPixels()];
    	for (int pixel=0; pixel<this.getNumPixels(); pixel++) {
      	  if (layer==1) dist[pixel] = this.getUdist(pixel+1);
    	  if (layer==2) dist[pixel] = this.getVdist(pixel+1);
    	  if (layer==3) dist[pixel] = this.getWdist(pixel+1);
    	}
    	return dist;
    }
    
    public double getDist(int layer, int pixel) {
    	double dist = 0;
    	if (layer==1) dist = this.getUdist(pixel);
  	  	if (layer==2) dist = this.getVdist(pixel);
  	  	if (layer==3) dist = this.getWdist(pixel);
  	  	return dist;
    }
    
    public int getNumPixels() {
    	return pixelNumber.getMap().size();
    }
    
    public DetectorShape2D getShape(int pixel) {
    	return getPixel(pixel).shape;
    }
    
    public double getArea(int pixel) {
    	return getPixel(pixel).area;
    }
    
    public int getZone(int pixel) {
        return getPixel(pixel).zone;
    }
    
    public void findMaxPixelArea(Pixel pixel) {
        double a = pixel.getArea();
        int    z = pixel.getZone();
        if(a>maxZonePixelArea[z]) maxZonePixelArea[z]=a;    
        if(a>maxPixelArea) maxPixelArea=a;
    }
    
    public double getMaxZonePixelArea(int pixel){
        return maxZonePixelArea[getZone(pixel)];
    }
    
    public double getZoneNormalizedArea(int pixel) {
    	return getArea(pixel)/getMaxZonePixelArea(pixel);
    }
    
    public double getMaxPixelArea(int pixel){
        return maxPixelArea;
    }
    
    public double getNormalizedArea(int pixel) {
        return getArea(pixel)/getMaxPixelArea(pixel);
    }
    
    public int[] getStrips(int pixel) {
    	return getPixel(pixel).strips;		
    }
   
    public int getStrip(int layer, int pixel) {
    	int[] str = getStrips(pixel);
    	return str[layer-1];	
    }
    
    public H1F getH1D(String name, int pixel) {
        return getPixel(pixel).h1d.get(name);
    }
    
    public H2F getH2D(String name, int pixel) {
        return getPixel(pixel).h2d.get(name);
    }

}
