/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.fcmon.detector.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
//import org.jlab.detector.view.DetectorListener;
//import org.jlab.detector.view.DetectorShape2D;
import org.jlab.detector.view.ViewWorld;
import org.jlab.geom.prim.Path3D;

/**
 *
 * @author gavalian
 * @version Modified by lcsmith for use with ECMon
 */
public class DetectorShape2D {
    
    DetectorDescriptor         desc = new DetectorDescriptor();
    Path3D                shapePath = new Path3D();
    
    int                 colorRed    = 213;
    int                 colorGreen  = 246;
    int                 colorBlue   = 230;
    
    int                 colorAlpha  = 255;
    int                 counter     = 0;
    int                 lineWidth   = 2;
    
    double                       xc = 0;
    double                       yc = 0;
    
    String              shapeTitle  = "";
    
    DetectorShape2D     activeShape = null;
    
    List<DetectorListener>    detectorListeners = new ArrayList<DetectorListener>();
        
    public DetectorShape2D(){     
    }
    
    public DetectorShape2D(DetectorType type, int sector, int layer, int component, int order){
        this.desc.setType(type);
        this.desc.setSectorLayerComponent(sector, layer, component);
        this.desc.setOrder(order);
        this.shapeTitle = String.format("DETECTOR %s SECTOR %4d LAYER %4d  PMT %4d  ORDER %4d",
                type.getName(),sector,layer,component,order);
    }
    
    public void addDetectorListener(DetectorListener lt){
        this.detectorListeners.add(lt);
    }
    
    public DetectorDescriptor  getDescriptor(){ return desc;}
    public Path3D              getShapePath(){ return shapePath;}
    
    public void setColor(int r, int g, int b){
        this.colorRed   = r;
        this.colorGreen = g;
        this.colorBlue  = b;
        this.colorAlpha = 255;
    }
    
    public void setColor(int r, int g, int b, int alpha){
        this.colorRed   = r;
        this.colorGreen = g;
        this.colorBlue  = b;
        this.colorAlpha = alpha;
    }
    
    public Color getSwingColor(){
        return new Color(this.colorRed,this.colorGreen,this.colorBlue,this.colorAlpha);
    }
    
    public Color getSwingColorWithAlpha(int alpha){
        return new Color(this.colorRed,this.colorGreen,this.colorBlue,alpha);
    }
    
    public void createBarXY(double width, double height){
        this.shapePath.clear();
        this.shapePath.addPoint(-width/2.0, -height/2.0,0.0);
        this.shapePath.addPoint(-width/2.0,  height/2.0,0.0);
        this.shapePath.addPoint( width/2.0,  height/2.0,0.0);
        this.shapePath.addPoint( width/2.0, -height/2.0,0.0);        
    }
    
    public void createTrapXY(double dx1, double dx2, double dy){
        this.shapePath.clear();
        this.shapePath.addPoint(-dx1/2.0,  dy/2.0,  0.0);
        this.shapePath.addPoint( dx1/2.0,  dy/2.0,  0.0);
        this.shapePath.addPoint( dx2/2.0, -dy/2.0,  0.0);
        this.shapePath.addPoint(-dx2/2.0, -dy/2.0,  0.0);
    }
    
    public void createSplitTrapXY(int split, double dx1, double dx2, double dy){
        this.shapePath.clear();
        switch (split) {
        case 0: this.shapePath.addPoint(-dx1/2.0,  dy/2.0,  0.0);
                this.shapePath.addPoint(      0.,  dy/2.0,  0.0);
                this.shapePath.addPoint(      0., -dy/2.0,  0.0);
                this.shapePath.addPoint(-dx2/2.0, -dy/2.0,  0.0); break;
        case 1: this.shapePath.addPoint(      0.,  dy/2.0,  0.0);
                this.shapePath.addPoint( dx1/2.0,  dy/2.0,  0.0);
                this.shapePath.addPoint( dx2/2.0, -dy/2.0,  0.0);
                this.shapePath.addPoint(      0., -dy/2.0,  0.0);
        }
    } 
    
    public void createCirc(double x0, double y0, double radius) {
    	this.shapePath.clear();
    	int numberOfPoints = 36;
    	double twopi = 2*3.1415926;
        double step = twopi/numberOfPoints;
        
        for(double angle = 0; angle < twopi; angle+=step){
            this.shapePath.addPoint(
                    x0+radius*Math.cos(Math.toRadians(angle)),
                    y0+radius*Math.sin(Math.toRadians(angle)),
                    0.0);
        } 
    }
    
    public void createArc(double radiusInner, double radiusOuter,
            double angleStart, double angleEnd){
        
        this.shapePath.clear();
        int   numberOfPoints = 80;
        
        this.shapePath.addPoint(
                radiusInner*Math.cos(Math.toRadians(angleStart)),
                radiusInner*Math.sin(Math.toRadians(angleStart)),
                0.0);
        
        this.shapePath.addPoint(
                radiusOuter*Math.cos(Math.toRadians(angleStart)),
                radiusOuter*Math.sin(Math.toRadians(angleStart)),
                0.0);
        
        double step = (angleEnd - angleStart)/numberOfPoints;
        for(double angle = angleStart; angle < angleEnd; angle+=step){
            this.shapePath.addPoint(
                    radiusOuter*Math.cos(Math.toRadians(angle)),
                    radiusOuter*Math.sin(Math.toRadians(angle)),
                    0.0);
        }
        
        this.shapePath.addPoint(
                radiusOuter*Math.cos(Math.toRadians(angleEnd)),
                radiusOuter*Math.sin(Math.toRadians(angleEnd)),
                0.0);
        this.shapePath.addPoint(
                radiusInner*Math.cos(Math.toRadians(angleEnd)),
                radiusInner*Math.sin(Math.toRadians(angleEnd)),
                0.0);
        for(double angle = angleEnd; angle > angleStart; angle-=step){
            this.shapePath.addPoint(
                    radiusInner*Math.cos(Math.toRadians(angle)),
                    radiusInner*Math.sin(Math.toRadians(angle)),
                    0.0);
        }
    }
    
    public void     reset() {this.counter = 0; this.xc=0 ; this.yc=0;}    
    public int getCounter() {return counter;}
    
    public DetectorShape2D setCounter(int c, double x, double y){
        this.counter = c ; 
        this.xc = x;
        this.yc = y;
        return this;
    }
    
    public void setColorByStatus(int status){
        int rs = status;
        if(status>10) rs = 10;
        if(status<0)  rs = 0;
        int red   = (25*rs);
        int green = (255-red);
        System.out.println(" setting color " + red + " " + green + " 0");
        this.setColor(red,green,0);
    }
    
    public boolean isContained(double x, double y){
        int i, j;
        boolean c = false;
        int nvert = shapePath.size();
        for (i = 0, j = nvert-1; i < nvert; j = i++) {
            if ( (( shapePath.point(i).y()>y) != (shapePath.point(j).y()>y)) &&
                    (x < ( shapePath.point(j).x()-shapePath.point(i).x()) * 
                    (y-shapePath.point(i).y()) / (shapePath.point(j).y()-shapePath.point(i).y()) +
                    shapePath.point(i).x()))
                c = !c;
        }
        return c;
    }
    
    
    public void drawShape(Graphics2D g2d, ViewWorld world, Color fillcolor, Color strokecolor){
        GeneralPath path = new GeneralPath();
        if(this.shapePath.size()>0){
            double xp = shapePath.point(0).x();
            double yp = shapePath.point(0).y();
            
            path.moveTo(world.getPointX(xp),world.getPointY(yp));
            for(int i = 1; i < shapePath.size(); i++){
                xp = shapePath.point(i).x();
                yp = shapePath.point(i).y();
                path.lineTo(world.getPointX(xp),world.getPointY(yp));
            }            
            xp = shapePath.point(0).x();
            yp = shapePath.point(0).y();           
            path.lineTo(world.getPointX(xp),world.getPointY(yp));
            
            g2d.setColor(fillcolor);
            g2d.fill(path);
            g2d.setColor(strokecolor);
            //g2d.setStroke(new BasicStroke(2));
            g2d.draw(path);
            
            if(this.counter>0) {
                this.setColor(255, 0, 0);
                g2d.setColor(this.getSwingColor());
                g2d.fill(path);
            }
        }        
    }
    
    public GeneralPath  getGeneralPath(){
        return new GeneralPath();
    }
 
}
