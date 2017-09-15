/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.fcmon.detector.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.view.ViewWorld;
import org.jlab.geom.prim.Path3D;
import org.jlab.groot.base.ColorPalette;
import org.jlab.groot.graphics.GraphicsAxis;
import org.jlab.groot.math.Dimension1D;
import org.jlab.groot.math.Dimension2D;
import org.jlab.utils.groups.IndexedList;

/**
 *
 * @author gavalian
 * @version Modified by lcsmith for use with ECMon
 */
@SuppressWarnings("serial")
public class DetectorView2D extends JPanel implements MouseMotionListener {
    
    Map<String,DetectorViewLayer2D>  viewLayers = new LinkedHashMap<String,DetectorViewLayer2D>();
    Dimension2D                      viewBounds = new Dimension2D();
    List<String>                 viewLayerNames = new ArrayList<String>();
    ViewWorld                             world = new ViewWorld();
    DetectorShape2D                 activeShape = null;
    String                          activeLayer = null;
    Color                       backgroundColor = Color.GRAY;
    GraphicsAxis                    colorAxis   = new GraphicsAxis(2);
    List<DetectorListener>    detectorListeners = new ArrayList<DetectorListener>();
    
    public double zmin=0;
    public double zmax=10;
    
    int delay;    
    Timer timer;    
    static final int FPS_INIT = 10; 
    
    // lcs: Don't paint unless new shape entered
    int selectedShape = -1;
    int selectedShapeSave = -1;
    
    private boolean       isMouseMotionListener = true;
    
    public DetectorView2D(){        
        super();
        this.setSize(new Dimension(500,500));
        this.setMinimumSize(new Dimension(200,200));
        addListeners();
        updateGUIAction action = new updateGUIAction();
        delay = 1000 / FPS_INIT;
        this.timer = new Timer(delay,action);  
    }
    
    private void addListeners(){
        if(this.isMouseMotionListener==true){
            this.addMouseMotionListener(this);
        }
    }
    
    public void addDetectorListener(DetectorListener lt){
        this.detectorListeners.add(lt);
    }  
    
    public void fill(List<DetectorDataDgtz> data, String options){
        for(Map.Entry<String,DetectorViewLayer2D> entry : this.viewLayers.entrySet()){
            entry.getValue().fill(data, options);
        }
    }
    
    public void start(int fps){
        delay = 10000;
        if (fps!=0 ) delay = 1000 / fps;
        this.timer.setDelay(delay);
        this.timer.start();
    }
    
    public void stop(){
        this.timer.stop();
    } 
    
    private class updateGUIAction implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            updateGUI();
        }
    }
    
    public void updateGUI(){
        if (activeShape!=null) for(DetectorListener lt : this.detectorListeners) lt.processShape(activeShape);      
        this.repaint();
    }  
    
    @Override
    public void paint(Graphics g){ 

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  //KEY_ANTIALIASING too slow (~130 ms for PCAL)
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);      
        
        int w = this.getSize().width;
        int h = this.getSize().height;
        
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, w, h);
        
        this.viewBounds.getDimension(0).setMinMax(0, w);
        this.viewBounds.getDimension(1).setMinMax(0, h);
        //g2d.setColor(Color.red);
        //g2d.drawRect(10,10,w-20,h-20);
       //long startTime = System.currentTimeMillis();
        this.drawLayers(g2d);
       //System.out.println("initDetector() time= "+(System.currentTimeMillis()-startTime));
        this.colorAxis.setVertical(true);
        this.colorAxis.setAxisType(GraphicsAxis.AXISTYPE_COLOR);
        this.colorAxis.setDimension(h-20,h-120);
        this.colorAxis.setRange(zmin,zmax);
        this.colorAxis.drawAxis(g2d, 10, h-20);
        
    }
    
    public List<String> getLayerNames(){ return this.viewLayerNames;}
    
    public void drawLayers(Graphics2D g2d){
        
        int w = this.getSize().width;
        int h = this.getSize().height;
        
        Dimension2D  commonDimension = new Dimension2D();        
        
        int n = 0; //lcs: set dimension for first view only

        for(Map.Entry<String,DetectorViewLayer2D> entry : this.viewLayers.entrySet()){
            //System.out.println("[Drawing] ---> layer : " + entry.getKey() + " " + 
            //                                               entry.getValue().getBounds());
            if (n==0) {
                commonDimension.copy(entry.getValue().getBounds());
                commonDimension.getDimension(0).addPadding(0.1);
                commonDimension.getDimension(1).addPadding(0.1);
                world.setWorld(viewBounds);
                world.setView(commonDimension);
                n++;
            }
            
            //world.show();
            if(entry.getValue().isActive()==true) entry.getValue().drawLayer(g2d, world);
        }
        
        if (viewLayers.containsKey("L0")) {
            if(activeLayer.equals("PIX0")) viewLayers.get("L0").drawLines(g2d, world);
            if(activeLayer.equals("PIX1")) viewLayers.get("L1").drawLines(g2d, world);
            if(activeLayer.equals("PIX2")) viewLayers.get("L2").drawLines(g2d, world);
        }
                
    }
    
    public void changeBackground(Color bkg){
        System.out.println("background color change ");
        this.backgroundColor = bkg;
    }
    
    public void removeLayer(String name){
        if(this.viewLayers.containsKey(name)==true){
            this.viewLayers.remove(name);
            this.viewLayerNames.remove(name);
        }
    }
    
    public void addLayer(String name){
        if(this.viewLayers.containsKey(name)==true){
            
        } else {
            this.viewLayers.put(name, new DetectorViewLayer2D());
            this.viewLayerNames.add(name);
        }
    }
    
    public void addShape(String layer, DetectorShape2D shape){
        if(this.viewLayers.containsKey(layer)==false){
            addLayer(layer);
        }        
        this.viewLayers.get(layer).addShape(shape);
        this.activeShape = shape;
    }
    
    public void setOpacity (String layer, int value){
        this.viewLayers.get(layer).setOpacity(value);
    }

    public boolean isLayerActive(String layer){
        return this.viewLayers.get(layer).isActive();
    }
    
    public void setHitMap(boolean flag){
        for(String layer : this.viewLayerNames){
            this.viewLayers.get(layer).setShowHitMap(flag);
        }
    }
    
    public void  setLayerState(String layer, boolean flag){
        if(activeLayer!=null&&isLayerActive(activeLayer)) viewLayers.get(activeLayer).setActive(false);
        viewLayers.get(layer).setActive(flag);
        activeLayer = layer;
        for(Map.Entry<Long,DetectorShape2D>  shape : viewLayers.get(layer).shapes.getMap().entrySet()){        
          activeShape = shape.getValue();
          break;
        }

    }
    
    public void    setLayerActive(String layer) {viewLayers.get(layer).setActive(true);}
 
    public void  setLayerInActive(String layer) {viewLayers.get(layer).setActive(false);}
    
    public void setDetectorListener(String layer, DetectorListener dl) {
        viewLayers.get(layer).addDetectorListener(dl);
    }  
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int index = -1;
        if(this.isMouseMotionListener==true) {
            double x = world.getViewX(e.getX());
            double y = world.getViewY(e.getY());
            DetectorShape2D selection = null;
            for(String layer : this.viewLayerNames){            
                if(viewLayers.get(layer).isActive()==true){
                    this.viewLayers.get(layer).resetSelection();
                    selection = this.viewLayers.get(layer).getShapeByXY(x,y);
                    if(selection!=null) {
                        index = selection.hashCode();
                        this.viewLayers.get(layer).setSelected(selection);
                        break;
                    }
                }
            } 

            if(selection!=null&&index!=this.selectedShapeSave){
                if(activeShape!=null){
                   // System.out.println(" compare = " + activeShape.getDescriptor().compare(selection.getDescriptor()));
                   // System.out.println(" active shape = " + selection.getDescriptor());
                }
               // System.out.println(" SELECTION = " + selection.getDescriptor());
                this.selectedShape = index;
                this.selectedShapeSave = index;
                activeShape = selection;
                for(DetectorListener lt : this.detectorListeners) lt.processShape(activeShape);
                repaint();
           }
        }
    }
    /**
     * Layer class to keep shapes in. it computes it's boundaries automatically;
     */
    public static class DetectorViewLayer2D {
        
        private IndexedList<DetectorShape2D>   shapes = null;
        private String                      layerName = "Layer";
        private Dimension2D                boundaries = new Dimension2D(); 
        private int                      layerOpacity = 255;
        private DetectorDescriptor selectedDescriptor = new DetectorDescriptor();
        private boolean                 isLayerActive = false;
        private Dimension1D                 axisRange = new Dimension1D();
        private boolean                    showHitMap = false;
        private int                           opacity = 255;
        private ColorPalette                  palette = new ColorPalette();
        private Color                     strokeColor = Color.black;        
        List<DetectorListener>    detectorListeners = new ArrayList<DetectorListener>();
        
        public DetectorViewLayer2D() {
            shapes = new IndexedList<DetectorShape2D>(4);
        }
        
        public void addDetectorListener(DetectorListener lt){
            this.detectorListeners.add(lt);
        }
        
        public DetectorViewLayer2D addShape(DetectorShape2D shape){
            
            int  type       = shape.getDescriptor().getType().getDetectorId();
            int  sector     = shape.getDescriptor().getSector();
            int  layer      = shape.getDescriptor().getLayer();
            int  component  = shape.getDescriptor().getComponent();
            int  order      = shape.getDescriptor().getOrder();
            
            if(shapes.getMap().isEmpty()){
                boundaries.set(
                        shape.getShapePath().point(0).x(),
                        shape.getShapePath().point(0).x(),
                        shape.getShapePath().point(0).y(),
                        shape.getShapePath().point(0).y()
                        );
            }
            
            int npoints = shape.getShapePath().size();
            for(int i = 0; i < npoints; i++){
                boundaries.grow(
                        shape.getShapePath().point(i).x(),
                        shape.getShapePath().point(i).y()
                );
            }
            //boundaries.getDimension(0).addPadding(0.1);
            //boundaries.getDimension(1).addPadding(0.1);
            
            this.shapes.add(shape,sector,layer,component,order);
            return this;
        }
           
        public int                  getOpacity()                {return opacity;}
        public boolean              isActive()                  {return this.isLayerActive;}
        public DetectorViewLayer2D  setActive(boolean flag)     {isLayerActive = flag;return this;}
        public DetectorViewLayer2D  setOpacity(int op)          {this.opacity = op;return this;}
        public DetectorViewLayer2D  setShowHitMap(boolean flag) {this.showHitMap = flag;return this;}      
        public String               getName()                   {return this.layerName;}    
        
        public final DetectorViewLayer2D setName(String name){
            this.layerName = name;
            return this;
        }
        
        public void setSelected(DetectorShape2D shape){
            this.selectedDescriptor.copy(shape.getDescriptor());
            this.selectedDescriptor.setOrder(shape.getDescriptor().getOrder());
        }
        
        public void resetSelection(){
            this.selectedDescriptor.setCrateSlotChannel(0, 0, 0);
            this.selectedDescriptor.setSectorLayerComponent(0, 0, 0);
            this.selectedDescriptor.setOrder(0);
            this.selectedDescriptor.setType(DetectorType.UNDEFINED);
        }
        
        public DetectorShape2D  getShapeByXY(double x, double y){
            for(Map.Entry<Long,DetectorShape2D>  shape : shapes.getMap().entrySet()){                
                if(shape.getValue().isContained(x, y)==true) return shape.getValue();
            }
            return null;
        }
        
        public Dimension1D  getAxisRange(){
            int counter = 0;
            for(Map.Entry<Long,DetectorShape2D>  shape : shapes.getMap().entrySet()){
                if(counter==0) axisRange.setMinMax(shape.getValue().getCounter(), shape.getValue().getCounter());
                axisRange.grow(shape.getValue().getCounter());
            }
            return this.axisRange;
        }
        
        public Dimension2D  getBounds(){
            return this.boundaries;
        }

        public void fill(List<DetectorDataDgtz> detectorData, String options){            
            boolean doReset = true;
            if(options.contains("same")==true) doReset = false;
            for(Map.Entry<Long,DetectorShape2D>  shape : shapes.getMap().entrySet()){
                if(doReset==true){ shape.getValue().reset(); }                
                DetectorDescriptor dm = shape.getValue().getDescriptor();
                
                for(int d = 0 ; d < detectorData.size(); d++){                     
                    DetectorDescriptor dd = detectorData.get(d).getDescriptor();
                    if(dd.getType()==dm.getType() &
                       dd.getSector()==dm.getSector() &
                       dd.getLayer()==dm.getLayer() &
                       dd.getComponent()==dm.getComponent()               
                            ){                            
                        //System.out.println("COLORING COMPONENT " + shape.getValue().getDescriptor());
                        int cv = shape.getValue().getCounter();
                        shape.getValue().setCounter(cv+1,0.,0.);
                    }
                }
            }
        }
        
        public void drawLines(Graphics2D g2d, ViewWorld world){
            
            for(Map.Entry<Long,DetectorShape2D> entry : this.shapes.getMap().entrySet()){           
                int x0 = (int) world.getPointX(entry.getValue().shapePath.point(0).x());
                int y0 = (int) world.getPointY(entry.getValue().shapePath.point(0).y());
                int x1 = (int) world.getPointX(entry.getValue().shapePath.point(1).x());
                int y1 = (int) world.getPointY(entry.getValue().shapePath.point(1).y());
                entry.getValue().setColor(255,0,0);
                g2d.setColor(entry.getValue().getSwingColor());
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(x0, y0, x1, y1);                
                g2d.setStroke(new BasicStroke(0));
            }
                        
        }
        
        public void drawLayer(Graphics2D g2d, ViewWorld world){
            
            List<DetectorShape2D> hits = new ArrayList<DetectorShape2D>();
            
            for(Map.Entry<Long,DetectorShape2D> entry : this.shapes.getMap().entrySet()){
                
                DetectorShape2D shape = entry.getValue();
                for(DetectorListener lt : this.detectorListeners) lt.update(shape);
                
                Color shapeColor = shape.getSwingColorWithAlpha(this.opacity);
                
                if (shape.getCounter()>0) hits.add(shape);
                
                Boolean matchOrder = this.selectedDescriptor.getOrder()==shape.getDescriptor().getOrder(); //new

                if(matchOrder&&this.selectedDescriptor.compare(shape.getDescriptor())==true){
                    shape.drawShape(g2d, world, Color.red, Color.black);
                } else {                 
                    shape.drawShape(g2d, world, shapeColor, Color.black);                        
                }
            }
                      
            for(DetectorShape2D shape : hits){
                double x = world.getPointX(shape.xc);
                double y = world.getPointY(shape.yc);
                shape.setColor(150,150,255);
                g2d.setColor(shape.getSwingColor());
                g2d.setStroke(new BasicStroke(2));
                g2d.fillOval((int) x, (int) y,10, 10);
                g2d.setStroke(new BasicStroke(0));           
            }
            
        }
        
    }
}
