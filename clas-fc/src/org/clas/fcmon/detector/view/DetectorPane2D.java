/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.fcmon.detector.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.SoftBevelBorder;

import org.clas.fcmon.tools.FCApplication;
import org.clas.fcmon.tools.FCDetector;

/**
 *
 * @author gavalian
 * @version Modified by lcsmith for use with ECMon
 */
public class DetectorPane2D extends JPanel {
    
    JPanel               mapPane = null;
    JPanel              viewPane = null;
    DetectorView2D        view2D = new DetectorView2D();
    
    public TreeMap<String,JPanel>  rbPanes = new TreeMap<String,JPanel>();
    public TreeMap<String,String>  bStore = new TreeMap<String,String>();
    
    List<List<buttonMap>>    viewStore = new ArrayList<List<buttonMap>>();
    List<List<buttonMap>>     mapStore = new ArrayList<List<buttonMap>>();
    
    List<FCApplication>   FCAListeners = new ArrayList<FCApplication>();
    List<FCDetector>      FCDListeners = new ArrayList<FCDetector>();
    
    buttonMap         lastMapButton = null;
           
    public DetectorPane2D(){
        super();
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createSoftBevelBorder(SoftBevelBorder.RAISED));
        makeGUI();
    }
    
    public void addFCApplicationListeners(FCApplication lt){
        this.FCAListeners.add(lt);
    }  
    
    public void addFCDetectorListeners(FCDetector lt){
        this.FCDListeners.add(lt);
    }  
    
    public final void makeGUI(){
         mapPane = new JPanel();
        viewPane = new JPanel();  
        viewPane.setBorder(BorderFactory.createSoftBevelBorder(SoftBevelBorder.LOWERED));
        this.add( mapPane,BorderLayout.PAGE_START);
        this.add(  view2D,BorderLayout.CENTER);
        this.add(viewPane,BorderLayout.PAGE_END);        
    }

    public buttonMap getViewButtonMap(int groupIndex, int nameIndex) {
        return viewStore.get(groupIndex).get(nameIndex);        
    }
    
    public buttonMap getMapButtonMap(int groupIndex, int nameIndex) {
        return mapStore.get(groupIndex).get(nameIndex);        
    }
    
    public class buttonMap {
       public String group;
       public String name;
       public int key;       
       public JRadioButton b;
       buttonMap() {}
       buttonMap(String group, String name, int key) {
           this.group = group;
            this.name = name;
             this.key = key;
               this.b = new JRadioButton(this.name);
//               this.b.setBackground(Color.LIGHT_GRAY);
       }
   }
    
    public void addViewStore(String group, List<String> name, List<Integer> key) {
        List<buttonMap> store = new ArrayList<buttonMap>();
        for(int i=0; i<name.size() ; i++) store.add(new buttonMap(group, name.get(i), key.get(i)));
        viewStore.add(store);
    }
    
    public void addMapStore(String group, List<String> name, List<Integer> key) {
        List<buttonMap> store = new ArrayList<buttonMap>();
        for(int i=0; i<name.size() ; i++) store.add(new buttonMap(group, name.get(i), key.get(i)));       
        mapStore.add(store);
    }
    
    public void selectViewButton(String group, String name) {
        for(List<buttonMap> bg: viewStore) {
            for(buttonMap bn: bg) {
                if (bn.group.equals(group)&&bn.name.equals(name)) {bn.b.doClick(); return;}
            }
        }
    }
    
    public Boolean checkViewButton(String group, String name) {
        for(List<buttonMap> bg: viewStore) {
            for(buttonMap bn: bg) {
                if (bn.group.equals(group)&&bn.name.equals(name)) return bn.b.isSelected();
            }
        }
        return false;
    }  
    
    public int getMapKey(String group, String name) {
        for(List<buttonMap> bg: mapStore) {
            for(buttonMap bn: bg) {
                if (bn.group.equals(group)&&bn.name.equals(name)) {return bn.key;}
            }
        } 
        return -1;
    }
    
    public void selectMapButton(String group, String name) {
        for(List<buttonMap> bg: mapStore) {
            for(buttonMap bn: bg) {
                if (bn.group.equals(group)&&bn.name.equals(name)) {bn.b.doClick(); return;}
            }
        }
    } 
    
    public void addMapButtons() {
        for(List<buttonMap> bg: mapStore) { 
            ButtonGroup bG = new ButtonGroup();
            String bGname = null;
            for(buttonMap bn: bg) {
                bGname = bn.group;
                if(!rbPanes.containsKey(bGname)) rbPanes.put(bGname, new JPanel());
                bn.b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        for(FCApplication lt : FCAListeners) lt.mapButtonAction(bn.group,bn.name,bn.key);
                        for(FCDetector    lt : FCDListeners) lt.mapButtonAction(bn.group,bn.name,bn.key);
                    }
                });
                this.rbPanes.get(bGname).add(bn.b); bG.add(bn.b);
            }
            this.mapPane.add(rbPanes.get(bGname));
        } 
    }
    
    public void addViewButtons() {
        for(List<buttonMap> bg: viewStore) {
            ButtonGroup bG = new ButtonGroup();
            for(buttonMap bn: bg) {
                bn.b.addActionListener(new ActionListener() {
                   public void actionPerformed(ActionEvent e) {
                       for(FCApplication lt : FCAListeners) lt.viewButtonAction(bn.group,bn.name,bn.key);                   
                       for(FCDetector    lt : FCDListeners) lt.viewButtonAction(bn.group,bn.name,bn.key);                     
                   }
                });               
                this.viewPane.add(bn.b); bG.add(bn.b);
            }
        } 
    }  
    
    public void setFPS(int fps){
        if (fps==0) getView().stop();
        if (fps>0)  getView().start(fps);
    }

    
    public DetectorView2D  getView(){
        return this.view2D;
    }
    
    public void update(){
        this.getView().repaint();
    }
    
}
