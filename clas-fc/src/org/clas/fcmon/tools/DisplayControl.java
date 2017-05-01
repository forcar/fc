package org.clas.fcmon.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.fcmon.detector.view.DetectorPane2D;
import org.jlab.groot.ui.RangeSlider;

public class DisplayControl extends JPanel {
    
    static final int FPS_MIN = 0;
    static final int FPS_MAX = 20;
    static final int FPS_INIT = 10;
    
    static final int SLIDER_MIN = 1;
    static final int SLIDER_MAX = 100;
    static final int SLIDER_MIN_INIT = 100;
    static final int SLIDER_MAX_INIT = 100;
              
    double zMin,zMax;
    
    public double pixMin = SLIDER_MIN_INIT*0.01;
    public double pixMax = SLIDER_MAX_INIT*0.01;
    
    JSlider framesPerSecond = new JSlider(JSlider.HORIZONTAL,FPS_MIN,FPS_MAX,FPS_INIT);

    RangeSlider      slider = new RangeSlider();
    DetectorPane2D detectorView;
    
	public void setPluginClass(DetectorPane2D detectorView) {    		 
		this.detectorView = detectorView;
	}
	    
    public DisplayControl() {        
        this.setBackground(Color.LIGHT_GRAY);
        getFramesPerSecond();
        getSliderPane();
    }
    
    public void setSliderLimits(int min, int max, int imin, int imax) {
        slider.setMinimum(min);
        slider.setMaximum(max);
        slider.setValue(imin);
        slider.setUpperValue(imax);                    
    }
    
    public void getSliderPane() {
        
        setSliderLimits(SLIDER_MIN,SLIDER_MAX,SLIDER_MIN_INIT,SLIDER_MAX_INIT);
        slider.setBackground(Color.LIGHT_GRAY);
        slider.setBorder(BorderFactory.createTitledBorder("Z-Range"));
        zMin = slider.getValue();
        zMax = slider.getUpperValue();
//        pixMin = Math.pow(10, zMin/10); pixMax = Math.pow(10, zMax/10);
        
        JLabel rangeSliderValue1 = new JLabel("" + String.format("%4.0f", zMin));
        JLabel rangeSliderValue2 = new JLabel("" + String.format("%4.0f", zMax));
        this.add(rangeSliderValue1);
        this.add(slider);
        this.add(rangeSliderValue2);     
        
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                RangeSlider slider = (RangeSlider) e.getSource();
                zMin = slider.getValue();
                zMax = slider.getUpperValue();
//                pixMin = Math.pow(10, zMin/10); pixMax = Math.pow(10, zMax/10);
                pixMin = 0.01*zMin; pixMax = Math.exp(-Math.pow(zMax-SLIDER_MAX,2)/2000.);  
                rangeSliderValue1.setText(String.valueOf("" + String.format("%4.0f", zMin)));
                rangeSliderValue2.setText(String.valueOf("" + String.format("%4.0f", zMax)));
                detectorView.getView().updateGUI();
            }
        });   
        
    }	
    
    public void getFramesPerSecond() {
        
        this.add(framesPerSecond);
        framesPerSecond.setBackground(Color.LIGHT_GRAY);
        framesPerSecond.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent f) {
                JSlider source = (JSlider)f.getSource();
                if (!source.getValueIsAdjusting()) {
                    int fps = (int)source.getValue(); 
                    detectorView.setFPS(fps); 
                }
            }               
        }
        );  
        
        framesPerSecond.setMajorTickSpacing(10);
        framesPerSecond.setMinorTickSpacing(1);
        framesPerSecond.setPaintTicks(true);
        framesPerSecond.setPaintLabels(true);
        framesPerSecond.setBorder(BorderFactory.createTitledBorder("FPS"));
        Font font = new Font("Serif", Font.ITALIC, 12);
        framesPerSecond.setFont(font);
        framesPerSecond.setPreferredSize(new Dimension(100,50));
                
    }
	
	public void setFPS(int fps){
	    framesPerSecond.setValue(fps);
	}
	
	
}
		
