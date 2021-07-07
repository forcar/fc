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
	import org.jlab.geom.component.ScintillatorPaddle;
	import org.jlab.geom.detector.ec.ECDetector;
	import org.jlab.geom.detector.ec.ECFactory;
	import org.jlab.geom.detector.ec.ECLayer;
	import org.jlab.geom.prim.Point3D;
	import org.freehep.math.minuit.FCNBase; 
	import org.freehep.math.minuit.FunctionMinimum; 
	import org.freehep.math.minuit.MnMigrad; 
	import org.freehep.math.minuit.MnPlot; 
	import org.freehep.math.minuit.MnScan; 
	import org.freehep.math.minuit.MnStrategy; 
	import org.freehep.math.minuit.MnUserParameters; 
	import org.freehep.math.minuit.Point; 

	public class geometryTest {

		public class oldFactory {			
			ECDetector          factory = new ECFactory().createDetectorSector(DataBaseLoader.getGeometryConstants(DetectorType.ECAL,10,"default"));
			ECLayer             ecLayer = null;
			
			public IndexedList<List<Point3D>> ind = new IndexedList<List<Point3D>>(4);
			
			void getAllVertices(int...indices) {
				for(int is=1; is<7; is++) {
					for(int det=1; det<4; det++) {
						for (int il=1; il<4; il++) {
							getVertices(is,det,il,indices);
						}
					}
				}				
			}
			
			void getVertices(int is, int det, int il, int...indices) {
				ecLayer = factory.getSector(is-1).getSuperlayer(det-1).getLayer(il-1);
				int ip = 0;
				for(ScintillatorPaddle paddle : ecLayer.getAllComponents()) {		
					ip++;
					if (!ind.hasItem(is,det,il,ip)) ind.add(new ArrayList<Point3D>(), is,det,il,ip);
				    for (int index:indices) {
						Point3D point = new Point3D();
				    	    point.copy(paddle.getVolumePoint(index)); 
				    	    point.rotateZ((is-1)*Math.PI/3.0);
				    	    ind.getItem(is,det,il,ip).add(point);
				    }
				}				
			}
		}
		
		public class newFactory {
			ConstantProvider         cp = GeometryFactory.getConstants(DetectorType.ECAL);
			public ECGeant4Factory   ecFactory = new ECGeant4Factory(cp);
			public PCALGeant4Factory pcFactory = new PCALGeant4Factory(cp);
			
			public IndexedList<List<Point3D>> ind = new IndexedList<List<Point3D>>(4);
			
			void getAllVertices(int...indices) {
				for(int is=1; is<7; is++) {
					for(int det=1; det<4; det++) {
						for (int il=1; il<4; il++) {
							getVertices(is,det,il,indices);
						}
					}
				}				
			}	
			
			void getVertices(int is, int det, int il, int...indices) {				
				switch (det) {
				case 1: getPCVertices(is, det, il, indices) ; break;
				case 2: getECVertices(is, det, il, indices) ; break;
				case 3: getECVertices(is, det, il+15, indices) ;
				}			
			}

			
			void getECVertices(int is, int det, int il, int...indices) {
				for (int ip=1; ip<ecFactory.getNumberOfPaddles()+1; ip++) {
					if (!ind.hasItem(is,det,il,ip)) ind.add(new ArrayList<Point3D>(), is,det,il,ip);
				    for (int index:indices) {
				    	    Point3D point = new Point3D();				
				    	    point.setX(ecFactory.getPaddle(is,il,ip).getVertex(index).x);
				    	    point.setY(ecFactory.getPaddle(is,il,ip).getVertex(index).y);
				    	    point.setZ(ecFactory.getPaddle(is,il,ip).getVertex(index).z);				    	    
				    	    ind.getItem(is,det,il,ip).add(point);
				    }
				}				
			}
			
			void getPCVertices(int is, int det, int il, int...indices) {
				for (int ip=1; ip<pcFactory.getNumberOfPaddles(il)+1; ip++) {
					if (!ind.hasItem(is,det,il,ip)) ind.add(new ArrayList<Point3D>(), is,det,il,ip);
				    for (int index:indices) {
				    	    Point3D point = new Point3D();
				    	    point.setX(pcFactory.getPaddle(is,il,ip).getVertex(index).x);
				    	    point.setY(pcFactory.getPaddle(is,il,ip).getVertex(index).y);
				    	    point.setZ(pcFactory.getPaddle(is,il,ip).getVertex(index).z);				    	   
				    	    ind.getItem(is,det,il,ip).add(point);
				    }
				}				
			}
			
		}
		
		public static void main(String[] args) {
			short setdetsector = 1;
			//PCal: 1, ECInner: 4, ECOuter: 7	
			short U = 0;
			short V = 1;
			short W = 2;
			
			short setdetECallayer = 1;
			short         setview = U;
			byte       setstripID = 1;
			
			newFactory fnew = new geometryTest().new newFactory();
			oldFactory fold = new geometryTest().new oldFactory();
			
			short detECallayer = setdetECallayer;
			short         view = setview;
			byte     ECallayer = (byte) (detECallayer+view);
			
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
			
			if(detECallayer != 1) // EC
				{
					int a = 0;
					{
						System.out.println(view+(5*detECallayer)-19+(3*a));
						
						float noPMTedgexa1 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(1).x;
						float noPMTedgexa3 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(3).x;
						float noPMTedgexa5 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(5).x;
						float noPMTedgexa7 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(7).x;
						float noPMTedgexa = (noPMTedgexa1+noPMTedgexa3+noPMTedgexa5+noPMTedgexa7)/4;
						noPMTedgextot = noPMTedgextot + noPMTedgexa;
						
						float noPMTedgeya1 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(1).y;
						float noPMTedgeya3 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(3).y;
						float noPMTedgeya5 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(5).y;
						float noPMTedgeya7 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(7).y;
						float noPMTedgeya = (noPMTedgeya1+noPMTedgeya3+noPMTedgeya5+noPMTedgeya7)/4;
						noPMTedgeytot = noPMTedgeytot + noPMTedgeya;
						
						float noPMTedgeza1 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(1).z;
						float noPMTedgeza3 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(3).z;
						float noPMTedgeza5 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(5).z;
						float noPMTedgeza7 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(7).z;
						float noPMTedgeza = (noPMTedgeza1+noPMTedgeza3+noPMTedgeza5+noPMTedgeza7)/4;
						noPMTedgeztot = noPMTedgeztot + noPMTedgeza;
						
						System.out.println("1= "+noPMTedgexa1+" "+noPMTedgeya1+" "+noPMTedgeza1);
						System.out.println("3= "+noPMTedgexa3+" "+noPMTedgeya3+" "+noPMTedgeza3);
						System.out.println("5= "+noPMTedgexa5+" "+noPMTedgeya5+" "+noPMTedgeza5);
						System.out.println("7= "+noPMTedgexa7+" "+noPMTedgeya7+" "+noPMTedgeza7);
						System.out.println(" ");
												
						float PMTedgexa0 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(0).x;
						float PMTedgexa2 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(2).x;
						float PMTedgexa4 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(4).x;
						float PMTedgexa6 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(6).x;
						float PMTedgexa = (PMTedgexa0+PMTedgexa2+PMTedgexa4+PMTedgexa6)/4;
						PMTedgextot = PMTedgextot + PMTedgexa;
						
						float PMTedgeya0 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(0).y;
						float PMTedgeya2 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(2).y;
						float PMTedgeya4 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(4).y;
						float PMTedgeya6 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(6).y;
						float PMTedgeya = (PMTedgeya0+PMTedgeya2+PMTedgeya4+PMTedgeya6)/4;
						PMTedgeytot = PMTedgeytot + PMTedgeya;
						
						float PMTedgeza0 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(0).z;
						float PMTedgeza2 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(2).z;
						float PMTedgeza4 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(4).z;
						float PMTedgeza6 = (float) fnew.ecFactory.getPaddle(setdetsector, view+(5*detECallayer)-19+(3*a), stripID).getVertex(6).z;
						float PMTedgeza = (PMTedgeza0+PMTedgeza2+PMTedgeza4+PMTedgeza6)/4;
						PMTedgeztot = PMTedgeztot + PMTedgeza;
						
						System.out.println("0= "+PMTedgexa0+" "+PMTedgeya0+" "+PMTedgeza0);
						System.out.println("2= "+PMTedgexa2+" "+PMTedgeya2+" "+PMTedgeza2);
						System.out.println("4= "+PMTedgexa4+" "+PMTedgeya4+" "+PMTedgeza4);
						System.out.println("6= "+PMTedgexa6+" "+PMTedgeya6+" "+PMTedgeza6);
						System.out.println(" ");											
					}
				}
			
				if(detECallayer == 1) // PCAL
				{
					int iv = W+1; iv=1;
					//fnew.getVertices(setdetsector,1,iv,1,3,5,7); //far end
					//fnew.getVertices(setdetsector,1,iv,0,2,4,6); //readout end
					fnew.getVertices(setdetsector,1,iv,1,0,2,3); //front face					
					fold.getVertices(setdetsector,1,iv,0,4,5,1); //front face
					Point3D point;
					
					point = fnew.ind.getItem(setdetsector,1,iv,strip).get(0);
					System.out.println("1= "+point.x()+" "+point.y()+" "+point.z());
					point = fnew.ind.getItem(setdetsector,1,iv,strip).get(1);
					System.out.println("0= "+point.x()+" "+point.y()+" "+point.z());
					point = fnew.ind.getItem(setdetsector,1,iv,strip).get(2);
					System.out.println("2= "+point.x()+" "+point.y()+" "+point.z());
					point = fnew.ind.getItem(setdetsector,1,iv,strip).get(3);
					System.out.println("3= "+point.x()+" "+point.y()+" "+point.z());
					System.out.println(" ");
					
					point = fold.ind.getItem(setdetsector,1,iv,strip).get(0);
					System.out.println("0= "+point.x()+" "+point.y()+" "+point.z());
					point = fold.ind.getItem(setdetsector,1,iv,strip).get(1);
					System.out.println("4= "+point.x()+" "+point.y()+" "+point.z());
					point = fold.ind.getItem(setdetsector,1,iv,strip).get(2);
					System.out.println("5= "+point.x()+" "+point.y()+" "+point.z());
					point = fold.ind.getItem(setdetsector,1,iv,strip).get(3);
					System.out.println("1= "+point.x()+" "+point.y()+" "+point.z());
					System.out.println(" ");											
					
					for(int a = 0; a <= 4; a++)
					{
						System.out.println(view+detECallayer+(3*a));
						float noPMTedgexa1 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(1).x;
						float noPMTedgexa3 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(3).x;
						float noPMTedgexa5 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(5).x;
						float noPMTedgexa7 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(7).x;
						float noPMTedgexa = (noPMTedgexa1+noPMTedgexa3+noPMTedgexa5+noPMTedgexa7)/4;
						if(view != W) noPMTedgextot = noPMTedgextot + noPMTedgexa;
						else PMTedgextot = PMTedgextot + noPMTedgexa;
						
						float noPMTedgeya1 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(1).y;
						float noPMTedgeya3 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(3).y;
						float noPMTedgeya5 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(5).y;
						float noPMTedgeya7 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(7).y;
						float noPMTedgeya = (noPMTedgeya1+noPMTedgeya3+noPMTedgeya5+noPMTedgeya7)/4;
						if(view != W) noPMTedgeytot = noPMTedgeytot + noPMTedgeya;
						else PMTedgeytot = PMTedgeytot + noPMTedgeya;
						
						float noPMTedgeza1 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(1).z;
						float noPMTedgeza3 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(3).z;
						float noPMTedgeza5 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(5).z;
						float noPMTedgeza7 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(7).z;
						float noPMTedgeza = (noPMTedgeza1+noPMTedgeza3+noPMTedgeza5+noPMTedgeza7)/4;
						if(view != W) noPMTedgeztot = noPMTedgeztot + noPMTedgeza;
						else PMTedgeztot = PMTedgeztot + noPMTedgeza;
						
						System.out.println("1= "+noPMTedgexa1+" "+noPMTedgeya1+" "+noPMTedgeza1);
						System.out.println("3= "+noPMTedgexa3+" "+noPMTedgeya3+" "+noPMTedgeza3);
						System.out.println("5= "+noPMTedgexa5+" "+noPMTedgeya5+" "+noPMTedgeza5);
						System.out.println("7= "+noPMTedgexa7+" "+noPMTedgeya7+" "+noPMTedgeza7);
						System.out.println(" ");
						
						float PMTedgexa0 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(0).x;
						float PMTedgexa2 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(2).x;
						float PMTedgexa4 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(4).x;
						float PMTedgexa6 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(6).x;
						float PMTedgexa = (PMTedgexa0+PMTedgexa2+PMTedgexa4+PMTedgexa6)/4;
						if(view != W) PMTedgextot = PMTedgextot + PMTedgexa;
						else noPMTedgextot = noPMTedgextot + PMTedgexa;
						
						float PMTedgeya0 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(0).y;
						float PMTedgeya2 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(2).y;
						float PMTedgeya4 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(4).y;
						float PMTedgeya6 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(6).y;
						float PMTedgeya = (PMTedgeya0+PMTedgeya2+PMTedgeya4+PMTedgeya6)/4;
						if(view != W) PMTedgeytot = PMTedgeytot + PMTedgeya;
						else noPMTedgeytot = noPMTedgeytot + PMTedgeya;
						
						float PMTedgeza0 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(0).z;
						float PMTedgeza2 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(2).z;
						float PMTedgeza4 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(4).z;
						float PMTedgeza6 = (float) fnew.pcFactory.getPaddle(setdetsector, view+detECallayer+(3*a), stripID).getVertex(6).z;
						float PMTedgeza = (PMTedgeza0+PMTedgeza2+PMTedgeza4+PMTedgeza6)/4;
						if(view != W) PMTedgeztot = PMTedgeztot + PMTedgeza;
						else noPMTedgeztot = noPMTedgeztot + PMTedgeza;
						System.out.println("0= "+PMTedgexa0+" "+PMTedgeya0+" "+PMTedgeza0);
						System.out.println("2= "+PMTedgexa2+" "+PMTedgeya2+" "+PMTedgeza2);
						System.out.println("4= "+PMTedgexa4+" "+PMTedgeya4+" "+PMTedgeza4);
						System.out.println("6= "+PMTedgexa6+" "+PMTedgeya6+" "+PMTedgeza6);
						System.out.println(" ");
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
/*				
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " PMT_x: " + PMTx);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " PMT_y: " + PMTy);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " PMT_z: " + PMTz);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " noPMT_x: " + noPMTx);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " noPMT_y: " + noPMTy);
				System.out.println("Sector" + setdetsector + " Layer " + ECallayer + " Strip " + setstripID + " noPMT_z: " + noPMTz);
				*/
		}
	}

