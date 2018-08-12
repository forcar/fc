package org.clas.fcmon.ftof;

public class FTOFConstants {
    
    public static float[] TOFFSET = {125,125};  // FADC/TDC offset in ns
    public static int IS1 = 0 ;
    public static int IS2 = 0 ;  
    
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
}
