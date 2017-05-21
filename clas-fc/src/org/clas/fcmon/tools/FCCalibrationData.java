/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.fcmon.tools;

import static java.lang.Integer.parseInt;
import java.util.ArrayList;
import java.util.TreeMap;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.GraphErrors;

/**
 *
 * @author fanchini
 * @modified lcsmith
 */
public class FCCalibrationData {
    HipoFile hipofile;
    int ioff=0;
    
    public void getFile(String filename){
        HipoFile file = new HipoFile(filename);
        this.hipofile = file;
    }
    
    public DetectorCollection getCollection(String dirname){
    	DetectorCollection DC = new DetectorCollection();
    	ArrayList arrayl = this.hipofile.getArrayList(dirname);
    	for (Object obj : arrayl) {
    		String obj_label = getObjlabel(obj);
    		ioff = (obj_label.contains("low")) ? 7:0; // Needed only for runs 1126,1127
    		DC.add(getIndex(obj_label,"_s"),getIndex(obj_label,"_l"),getIndex(obj_label,"_c"),obj);
    	}    
    return DC;
}    
        
    private String getObjlabel(Object obj){
        String obj_label ="";
        H1F h_1d; 
        H2F h_2d;
        GraphErrors g_err;
        
        if(obj instanceof H1F){
                h_1d = (H1F) obj;
                obj_label = h_1d.getName();
        }
        if(obj instanceof H2F){
                h_2d = (H2F) obj;
                obj_label = h_2d.getName();
        }

        if(obj instanceof GraphErrors){
                g_err = (GraphErrors) obj;
                obj_label = g_err.getName();
        }
        return obj_label;
    } 

    private int getIndex(String name, String lab){
    	int i1=name.indexOf(lab)+2+(lab=="_l"?ioff:0);  
        return parseInt(name.substring(i1,i1+1));
    }    

}
