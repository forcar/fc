package org.clas.fcmon.tools;

	import java.util.ArrayList;
	import java.util.List;

	import javax.swing.JFrame;

	import org.jlab.clas.physics.LorentzVector;
	import org.jlab.groot.data.GraphErrors;
	import org.jlab.groot.data.H1F;
	import org.jlab.groot.data.H2F;
	import org.jlab.groot.fitter.DataFitter;
	import org.jlab.groot.graphics.EmbeddedCanvas;
	import org.jlab.groot.math.F1D;
	import org.jlab.groot.ui.TCanvas;
	import org.jlab.io.base.DataBank;
	import org.jlab.io.base.DataEvent;
	import org.jlab.io.hipo.HipoDataSource;
	import org.jlab.utils.groups.IndexedList;
	import org.jlab.io.evio.EvioDataBank;
	import org.jlab.io.hipo.HipoDataBank;
	import org.jlab.detector.base.DetectorType;
	import org.jlab.detector.base.GeometryFactory;
	import org.jlab.detector.geant4.v2.ECGeant4Factory;
	import org.jlab.detector.geant4.v2.PCALGeant4Factory;
	import org.jlab.geom.base.ConstantProvider;
	import org.freehep.math.minuit.FCNBase; 
	import org.freehep.math.minuit.FunctionMinimum; 
	import org.freehep.math.minuit.MnMigrad; 
	import org.freehep.math.minuit.MnPlot; 
	import org.freehep.math.minuit.MnScan; 
	import org.freehep.math.minuit.MnStrategy; 
	import org.freehep.math.minuit.MnUserParameters; 
	import org.freehep.math.minuit.Point; 

	public class geometryTest {

		public static void main(String[] args) {
			short setdetsector = 5;
			//PCal: 1, ECInner: 4, ECOuter: 7	
			short setdetECallayer = 1;
			short U = 0;
			short V = 1;
			short W = 2;
			short setview = V;
			byte setstripID = 51;
			
			short detECallayer = setdetECallayer;
			short view = setview;
			byte ECallayer = (byte) (detECallayer+view);
			
			ConstantProvider cp = GeometryFactory.getConstants(DetectorType.ECAL);
			ECGeant4Factory factory = new ECGeant4Factory(cp);
			ConstantProvider cp1 = GeometryFactory.getConstants(DetectorType.ECAL);
			PCALGeant4Factory factory1 = new PCALGeant4Factory(cp1);
			
			IndexedList<Double> CalConststore = new IndexedList<>(2);
			
			int stripmax = 36;
			if(ECallayer == 1) stripmax = 68;
			if(ECallayer == 2) stripmax = 62;
			if(ECallayer == 3) stripmax = 62;
			
			int strip = setstripID;
				byte stripID = (byte) strip;
				
				float PMTedgextot = 0;
				float PMTedgeytot = 0;
				float PMTedgeztot = 0;
				float noPMTedgextot = 0;
				float noPMTedgeytot = 0;
				float noPMTedgeztot = 0;
				if(detECallayer != 1)
				{
					int a = 0;
					{
						float noPMTedgexa1 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(1).x;
						float noPMTedgexa3 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(3).x;
						float noPMTedgexa5 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(5).x;
						float noPMTedgexa7 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(7).x;
						float noPMTedgexa = (noPMTedgexa1+noPMTedgexa3+noPMTedgexa5+noPMTedgexa7)/4;
						noPMTedgextot = noPMTedgextot + noPMTedgexa;
						
						float noPMTedgeya1 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(1).y;
						float noPMTedgeya3 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(3).y;
						float noPMTedgeya5 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(5).y;
						float noPMTedgeya7 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(7).y;
						float noPMTedgeya = (noPMTedgeya1+noPMTedgeya3+noPMTedgeya5+noPMTedgeya7)/4;
						noPMTedgeytot = noPMTedgeytot + noPMTedgeya;
						
						float noPMTedgeza1 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(1).z;
						float noPMTedgeza3 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(3).z;
						float noPMTedgeza5 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(5).z;
						float noPMTedgeza7 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(7).z;
						float noPMTedgeza = (noPMTedgeza1+noPMTedgeza3+noPMTedgeza5+noPMTedgeza7)/4;
						noPMTedgeztot = noPMTedgeztot + noPMTedgeza;
						
						float PMTedgexa0 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(0).x;
						float PMTedgexa2 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(2).x;
						float PMTedgexa4 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(4).x;
						float PMTedgexa6 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(6).x;
						float PMTedgexa = (PMTedgexa0+PMTedgexa2+PMTedgexa4+PMTedgexa6)/4;
						PMTedgextot = PMTedgextot + PMTedgexa;
						
						float PMTedgeya0 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(0).y;
						float PMTedgeya2 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(2).y;
						float PMTedgeya4 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(4).y;
						float PMTedgeya6 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(6).y;
						float PMTedgeya = (PMTedgeya0+PMTedgeya2+PMTedgeya4+PMTedgeya6)/4;
						PMTedgeytot = PMTedgeytot + PMTedgeya;
						
						float PMTedgeza0 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(0).z;
						float PMTedgeza2 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(2).z;
						float PMTedgeza4 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(4).z;
						float PMTedgeza6 = (float) factory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(6).z;
						float PMTedgeza = (PMTedgeza0+PMTedgeza2+PMTedgeza4+PMTedgeza6)/4;
						PMTedgeztot = PMTedgeztot + PMTedgeza;
					}
				}
				if(detECallayer == 1)
				{
					for(int a = 0; a <= 4; a++)
					{
						float noPMTedgexa1 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(1).x;
						float noPMTedgexa3 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(3).x;
						float noPMTedgexa5 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(5).x;
						float noPMTedgexa7 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(7).x;
						float noPMTedgexa = (noPMTedgexa1+noPMTedgexa3+noPMTedgexa5+noPMTedgexa7)/4;
						if(view != W) noPMTedgextot = noPMTedgextot + noPMTedgexa;
						else PMTedgextot = PMTedgextot + noPMTedgexa;
						
						float noPMTedgeya1 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(1).y;
						float noPMTedgeya3 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(3).y;
						float noPMTedgeya5 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(5).y;
						float noPMTedgeya7 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(7).y;
						float noPMTedgeya = (noPMTedgeya1+noPMTedgeya3+noPMTedgeya5+noPMTedgeya7)/4;
						if(view != W) noPMTedgeytot = noPMTedgeytot + noPMTedgeya;
						else PMTedgeytot = PMTedgeytot + noPMTedgeya;
						
						float noPMTedgeza1 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(1).z;
						float noPMTedgeza3 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(3).z;
						float noPMTedgeza5 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(5).z;
						float noPMTedgeza7 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(7).z;
						float noPMTedgeza = (noPMTedgeza1+noPMTedgeza3+noPMTedgeza5+noPMTedgeza7)/4;
						if(view != W) noPMTedgeztot = noPMTedgeztot + noPMTedgeza;
						else PMTedgeztot = PMTedgeztot + noPMTedgeza;
						
						float PMTedgexa0 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(0).x;
						float PMTedgexa2 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(2).x;
						float PMTedgexa4 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(4).x;
						float PMTedgexa6 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(6).x;
						float PMTedgexa = (PMTedgexa0+PMTedgexa2+PMTedgexa4+PMTedgexa6)/4;
						if(view != W) PMTedgextot = PMTedgextot + PMTedgexa;
						else noPMTedgextot = noPMTedgextot + PMTedgexa;
						
						float PMTedgeya0 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(0).y;
						float PMTedgeya2 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(2).y;
						float PMTedgeya4 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(4).y;
						float PMTedgeya6 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(6).y;
						float PMTedgeya = (PMTedgeya0+PMTedgeya2+PMTedgeya4+PMTedgeya6)/4;
						if(view != W) PMTedgeytot = PMTedgeytot + PMTedgeya;
						else noPMTedgeytot = noPMTedgeytot + PMTedgeya;
						
						float PMTedgeza0 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(0).z;
						float PMTedgeza2 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(2).z;
						float PMTedgeza4 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(4).z;
						float PMTedgeza6 = (float) factory1.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(6).z;
						float PMTedgeza = (PMTedgeza0+PMTedgeza2+PMTedgeza4+PMTedgeza6)/4;
						if(view != W) PMTedgeztot = PMTedgeztot + PMTedgeza;
						else noPMTedgeztot = noPMTedgeztot + PMTedgeza;
					}
				}
				float PMTx = 0;
				float PMTy = 0;
				float PMTz = 0;
				float noPMTx = 0;
				float noPMTy = 0;
				float noPMTz = 0;
				if(detECallayer != 1)
				{
					/*
					PMTx = PMTedgextot/(detECallayer+1);
					PMTy = PMTedgeytot/(detECallayer+1);
					PMTz = PMTedgeztot/(detECallayer+1);
					noPMTx = noPMTedgextot/(detECallayer+1);
					noPMTy = noPMTedgeytot/(detECallayer+1);
					noPMTz = noPMTedgeztot/(detECallayer+1);
					*/
					PMTx = PMTedgextot;
					PMTy = PMTedgeytot;
					PMTz = PMTedgeztot;
					noPMTx = noPMTedgextot;
					noPMTy = noPMTedgeytot;
					noPMTz = noPMTedgeztot;
				}
				if(detECallayer == 1)
				{
					PMTx = PMTedgextot/5;
					PMTy = PMTedgeytot/5;
					PMTz = PMTedgeztot/5;
					noPMTx = noPMTedgextot/5;
					noPMTy = noPMTedgeytot/5;
					noPMTz = noPMTedgeztot/5;
				}
				
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " PMT_x: " + PMTx);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " PMT_y: " + PMTy);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " PMT_z: " + PMTz);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " noPMT_x: " + noPMTx);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " noPMT_y: " + noPMTy);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " noPMT_z: " + noPMTz);
		}
	}

