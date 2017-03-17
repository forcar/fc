package org.clas.fcmon.detector.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jlab.groot.graphics.EmbeddedCanvas;

public class EmbeddedCanvasTabbed extends JPanel implements MouseMotionListener, MouseListener {
    
    private JTabbedPane   tabbedPane = null; 
    public JPanel        actionPanel = null;  
    public JPopupMenu          popup = null;
    private int             popupPad = 0;
    public String     selectedCanvas = null;
    public String               name = null;
    
    private Map<String,EmbeddedCanvas>  tabbedCanvases = new LinkedHashMap<String,EmbeddedCanvas>();
   
    public EmbeddedCanvasTabbed() {
        super();
    }
    
    public EmbeddedCanvasTabbed(String name){
        super();
        this.setLayout(new BorderLayout());
        this.name = name;
        initUI(name);
        initMouse();
    }
    
    public final void initMouse(){
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
    }
    
    public void initUI(String name){
        
        tabbedPane  = new JTabbedPane();
        actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout());
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                 JTabbedPane tabbedPane = (JTabbedPane) e.getSource();
                 int index = tabbedPane.getSelectedIndex();
                 selectedCanvas = tabbedPane.getTitleAt(index);
            }
        });            
        this.add(tabbedPane,BorderLayout.CENTER);
        this.add(actionPanel,BorderLayout.PAGE_END);
        this.addCanvas(name);
        
    }
    
    public String getName() {
        return this.name;
    }
    
    public void addCanvas(String name){        
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        this.tabbedCanvases.put(name, canvas);
        tabbedPane.addTab(name, canvas);
        tabbedPane.addMouseListener(this);

    }  
    
    public EmbeddedCanvas getCanvas(String title){
        return this.tabbedCanvases.get(title);
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
         if (SwingUtilities.isRightMouseButton(e)) {
             //System.out.println("POP-UP coordinates = " + e.getX() + " " + e.getY() + "  pad = " + popupPad);
             System.out.println("Mouse clicked");
             popup.show(this, e.getX(), e.getY());                 
         }
    }  
    
    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
}