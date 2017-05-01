package org.clas.fcmon.ec;

import org.jlab.utils.groups.IndexedList;

public class ECConstants {
    
    public static int IS1 = 0 ;
    public static int IS2 = 0 ;  
    public static double[] SCALE  = {10,10,10,10,10,10,10,10,10}; // Fitter.ADC/SCALE is plotted and fitted in ECMon
    public static double[] SCALE5 = {10,10,10,5,5,5,5,5,5}; // Sector 5 ECAL uses EMI PMTs near max voltage
    public static double[] REF = {150,150,150,100,100,100,160,160,160}; //SCALE adjusted expected MIP position from FADC
    public static double[] MIP = {100,100,100,100,100,100,160,160,160}; //MIP Energy in MeV X 10
    
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
}
