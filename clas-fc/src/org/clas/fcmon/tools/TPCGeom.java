package org.clas.fcmon.tools;

/**
 * 
 * @author lcsmith
 * Adapted from Rafayel Paremuzjan clas12 trigger code
 * https://github.com/JeffersonLab/clas12-trigger/blob/master/src/TPCalGeom.cxx
 */

public class TPCGeom {
	
	public TPCGeom(double aU, double aV, double aW) {
		setGeometry(aU,aV,aW) ;// U, V and W are strip coordinates in the (0-36) range
	}
	    
	public void setGeometry(double aU, double aV, double aW) {
    	
		fSector = -1;
		    
		fU = aU*fdU/Math.sin(alpha_base);
		fV = aV*fdV/Math.sin(alpha_base);
		fW = aW*fdW/Math.sin(alpha_base);

		fDalitz = aU / fNUStrips + aV / fNVStrips + aW / fNWStrips - 2.;
		  
		fyU = fy0_U + aU * fdU;
		fxV = fx0_V + fV;
		fxW = fx0_W - fW;

		fy_UV = fyU;
		fx_UV = fxV - (fyh - fy_UV) / Math.tan(alpha_base);

		fy_UW = fyU;
		fx_UW = fxW + (fyh - fy_UV) / Math.tan(alpha_base);

		fx_VW = 0.5 * (fxV + fxW);
		fy_VW = fyh - (fxV - fx_VW) * Math.tan(alpha_base);

	}	
	
	public void SetSector(int aSect){

	   fSector = aSect;

	   double[] f_UW = GetHallCoordinates(fx_UW, fy_UW);
	   double[] f_UV = GetHallCoordinates(fx_UV, fy_UV);
	   double[] f_VW = GetHallCoordinates(fx_VW, fy_UV);
	   
	   fx_UW_Hall = f_UW[0]; fy_UW_Hall = f_UW[1] ; fz_UW_Hall = f_UW[2];
	   fx_UV_Hall = f_UV[0]; fy_UV_Hall = f_UV[1] ; fz_UV_Hall = f_UV[2];
	   fx_VW_Hall = f_VW[0]; fy_VW_Hall = f_VW[1] ; fz_VW_Hall = f_VW[2];
	}
	
	public double[] GetHallCoordinates(double ax, double ay){
	    
	    // ==== Going from local coordinates to L2 where, only PCAL is tilted by 25 deg
		
	    double yl1 = fyShift + ay;
	    double yl2 = yl1 * Math.cos(PCal_angle);
	    double xl2 = ax;
	    double zl2 = fz0 - yl2 * Math.sin(PCal_angle);
	        
	    // ==== Going from L2 to Global, this is basically rotation along the z axis
	    
	    double phi = (60 * fSector - 90) * d2r;
	    
	    double[] out = new double[3];
	    out[0] = xl2*Math.cos(phi) - yl2*Math.sin(phi);
	    out[1] = xl2*Math.sin(phi) + yl2*Math.cos(phi);
	    out[2] = zl2;
	    
	    return out;
	}
	
	public double GetX() {return fx;}
	public double GetY() {return fy;}
	
	public double GetDalitz() {return fDalitz;}
	public double GetY_UV() {return fy_UV;}
	public double GetX_UV() {return fx_UV;}
	public double GetY_UW() {return fy_UW;}
	public double GetX_UW() {return fx_UW;}
	public double GetY_VW() {return fy_VW;}
	public double GetX_VW() {return fx_VW;}
		  
	public double GetHallX_UV() {return fx_UV_Hall;}
	public double GetHallY_UV() {return fy_UV_Hall;}
	public double GetHallZ_UV() {return fz_UV_Hall;}
	public double GetHallX_UW() {return fx_UW_Hall;}
	public double GetHallY_UW() {return fy_UW_Hall;}
	public double GetHallZ_UW() {return fz_UW_Hall;}
	public double GetHallX_VW() {return fx_VW_Hall;}
	public double GetHallY_VW() {return fy_VW_Hall;}
	public double GetHallZ_VW() {return fz_VW_Hall;}
		  
	int fSector;
		    
	double fx, fy;        // local coordinates, x, y in a Cartesian system
	double fU, fV, fW;    // fU,fV, fW are along the edges of the triangle
	double fyU;           // y coordinate of a U Strip on the side of the triangle (y constant along a U strip). 
	double fxV;           // x coordinate of the V strip on the base of the triangle
	double fxW;           // x coordinate of the W strip on the base of the triangle
    
	double fDalitz;       // Dalitz, note 2 is subtracted, so it is expected to be peaked at 0

	double fx_Hall, fy_Hall, fz_Hall; // x, y and z coordinates in the Hall Frame where (0., 0., 0) is the target center and z is along th beamline
		  
	double fx_UV, fy_UV; // cluster coordinate calculated through U and V peaks
	double fx_UW, fy_UW; // cluster coordinate calculated through U and W peaks
	double fx_VW, fy_VW; // cluster coordinate calculated through V and W peaks

	double fx_UV_Hall, fy_UV_Hall, fz_UV_Hall; //cluster coordinate in the Hall Frame, calculated through U and V peaks
	double fx_UW_Hall, fy_UW_Hall, fz_UW_Hall; //cluster coordinate in the Hall Frame, calculated through U and V peaks
	double fx_VW_Hall, fy_VW_Hall, fz_VW_Hall; //cluster coordinate in the Hall Frame, calculated through U and V peaks

	static double d2r = 0.017453; 
	static double alpha_base = 62.89 * d2r; // The angle between the base and scales of the triangle
	static double PCal_angle = 25 * d2r;    // The angle between the PCal plane and the beamline
		
	static double fdU = 4.52;  // Average width of U strips in cm
	static double fdV = 4.505; // Average width of V strips in cm
	static double fdW = 4.51;  // Average width of W strips in cm

	static double fb = 394.2;                              // Length of the Base of the PCal triangle
	static double fh = 385.2;                              // Height of the PCal triangle
	static double fside = 0.5 * fb / Math.cos(alpha_base); // The length of a side of a triangle
	static double fxl =  fb / 2.;                          // x coordinate of the left vertex of the triangle
	static double fxr = -fb / 2.;                          // x coordinate of the right vertex of the triangle
	static double fyh =   94.4;                            // Y coordinate of the base of the triangle (All points have the same y)
	static double fyl = -290.8;                            // Y coordinate of the vertex closest to the beamline
	
	static int fNUStrips = 84; // Number of U strips
	static int fNVStrips = 78; // Number of V strips
	static int fNWStrips = 78; // Number of W strips
	static int fNUsingle = 52; // Number of single readout strips in U
	static int fNVsingle = 46; // Number of single readout strips in U
	static int fNWsingle = 46; // Number of single readout strips in U

	static double fy0_U = fyh - fNUStrips * fdU;                        // Y coordinate of the Shortest U strip
	static double fx0_V = fxl - fNVStrips * fdV / Math.sin(alpha_base); // X coordinate of the Shortest V strip 
	static double fx0_W = fxr + fNWStrips * fdW / Math.sin(alpha_base); // X coordinate of the Shortest W strip 
	static double fz0     = 764.1995467 + 5.; // Calculated as 6926/cos(25*TMath::DegToRad()), 5 I coming from comparing with GEMC
	static double fyShift = 322.9646840 + 9.220069; // Calculated as 6926*tan(25*TMath::DegToRad()), 9.22 is obtained through comparing to GEM
	
}
