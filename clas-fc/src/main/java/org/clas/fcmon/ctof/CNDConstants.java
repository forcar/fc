package org.clas.fcmon.ctof;

public class CNDConstants {
    
    public static double TOFFSET = 450;  // FADC/TDC offset in ns
    public static int IS1 = 1 ;
    public static int IS2 = 1 ;
    public static int NPADDLES = 48;
    //CCDB: /geometry/cnd
    public static double RADIUS1  = 29.00;
    public static double THICK   =  3.02;
    public static double WIDTH   =  3.21;    
    public static double     DX1 = WIDTH;
    public static double     DX2 = DX1*(1+THICK/RADIUS1);
    public static double      DY = THICK;
    public static double       R = RADIUS1+DY;
    public static double RADIUS2 = 29.0;
    public static double  THICK2 = 1.0;
    
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
    

}
