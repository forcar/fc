package org.clas.fcmon.ec;

import org.jlab.utils.groups.IndexedList;

public class ECConstants {
    
    public static int IS1 = 0 ;
    public static int IS2 = 0 ;  
    public static float  TOFFSET = 125;                                 // FADC/TDC offset in ns
    
    public static double[] AtoE   = {15,15,15,10,10,10,10,10,10};       // SCALED ADC to Energy in MeV
    public static double[] AtoE5  = {15,15,15,5,5,5,5,5,5};             // For Sector 5 ECAL
    public static double[] SCALE  = {10,10,10,10,10,10,10,10,10};       // Fitter.ADC/SCALE is plotted and fitted in ECMon
    public static double[] SCALE5 = {10,10,10,5,5,5,5,5,5};             // Sector 5 ECAL uses EMI PMTs near max voltage
    public static double[] REF = {150,150,150,100,100,100,160,160,160}; // SCALE adjusted expected MIP position from FADC
    public static double[] MIP = {100,100,100,100,100,100,160,160,160}; // MIP Energy in MeV X 10
    
    public final double[][] AL={{16,25},{16,25}};
    public final double[][] AS={{35,65},{30,60}};
    public final double[][] AR={{75,90},{75,90}};
    public final double[][] TL={{450,580},{0,100}};
    public final double[][] TS={{600,700},{100,200}};
    public final double[][] TR={{710,840},{220,300}};
    
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }

}
