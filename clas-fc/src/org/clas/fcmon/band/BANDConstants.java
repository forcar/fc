package org.clas.fcmon.band;

public class BANDConstants {
    
    public static float[] TOFFSET = {0,0};  // FADC/TDC offset in ns
    public static int IS1 = 0 ;
    public static int IS2 = 0 ; 
    public static float[] bandlen  = {163.7f,201.9f,51.2f,51.2f,201.9f};
    public static float[] bandwid  = {7.5f,7.5f,7.5f,7.5f,7.5f};
    public static float[] bandxoff = {0.f,0.f,75.35f,-75.35f,0.f};
    public static float[] bandyoff = {13,10,3,3,-3};
    public static   int[] bandlay  = {3,7,6,6,2};
    
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
}
