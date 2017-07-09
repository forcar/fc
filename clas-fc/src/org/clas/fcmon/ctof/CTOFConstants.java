package org.clas.fcmon.ctof;

public class CTOFConstants {
    
    public static double TOFFSET = 450;  // FADC/TDC offset in ns
    public static int IS1 = 1 ;
    public static int IS2 = 1 ;
    public static int NPADDLES = 48;
    //Table 4: ctof_geom.tex
    public static double RADIUS  = 25.11;
    public static double LENGTH  = 80.683;
    public static double THICK   =  3.02;
    public static double WIDTH   =  3.21;    
    public static double     DX1 = WIDTH;
    public static double     DX2 = DX1*(1+THICK/RADIUS);
    public static double      DY = THICK;
    public static double       R = RADIUS+DY;
    
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
    

}
