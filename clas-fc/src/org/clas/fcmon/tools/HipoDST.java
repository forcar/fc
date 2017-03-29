package org.clas.fcmon.tools;

import org.jlab.hipo.data.HipoEvent;
import org.jlab.hipo.data.HipoGroup;
import org.jlab.hipo.io.HipoReader;
import org.jlab.hipo.io.HipoWriter;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;

public class HipoDST {
	
	public static void readNtuple() {
		
//		HipoReader reader = new HipoReader();
		HipoDataSource reader = new HipoDataSource();
		reader.open("/Users/colesmith/hipo_test_ntuple.hipo");
		Integer current = reader.getCurrentIndex();
        Integer nevents = reader.getSize(); 
        System.out.println("Current event:"+current+" Nevents: "+nevents);
        for (int i=0; i<10; i++) {
            DataEvent event = reader.getNextEvent();
            event.show();
            if(event.hasBank("Event")==true) {              
            	DataBank bank = event.getBank("Event");
            	System.out.println("Entires="+bank.rows());
            }
		}	
		reader.close();
	}
	
	public static void writeNtuple(){
		    
	   HipoWriter writer = new HipoWriter();
	        
	   writer.defineSchema("Event", 20, "id/I:px/F:py/F:pz/F");
	        
	   writer.setCompressionType(2);
	   writer.open("/Users/colesmith/hipo_test_ntuple.hipo");
       int nevents = 500;
	        
	   for(int i = 0; i < nevents; i++){
	       HipoGroup bank = writer.getSchemaFactory().getSchema("Event").createGroup(4);
//	       for (int j=0; j < 2; j++) {
	           bank.getNode("id").setInt(0, 211);
	           bank.getNode("px").setFloat(0, (float) Math.random());
	           bank.getNode("py").setFloat(0, (float) Math.random());
	           bank.getNode("pz").setFloat(0, (float) Math.random());
//	       }
	       bank.show();
	       HipoEvent event = writer.createEvent();
	       event.writeGroup(bank);
	       writer.writeEvent(event);
	   }
	   writer.close();
	}

    public static void main(String[] args){        
        HipoDST.writeNtuple();
        HipoDST.readNtuple();
    }
    
}
