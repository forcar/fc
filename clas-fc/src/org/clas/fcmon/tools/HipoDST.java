package org.clas.fcmon.tools;

import org.jlab.hipo.data.HipoEvent;
import org.jlab.hipo.data.HipoGroup;
import org.jlab.hipo.io.HipoReader;
import org.jlab.hipo.io.HipoWriter;

public class HipoDST {
	
	public static void readNtuple() {
		
		HipoReader reader = new HipoReader();
		reader.open("/Users/colesmith/hipo_test_ntuple.hipo");
		int nevents=500;
		for (int i=0; i < 10; i++) {
	       HipoEvent event = reader.readHipoEvent(i);
	       event.show();
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
	       bank.getNode("id").setInt(0, 211);
	       bank.getNode("px").setFloat(0, (float) Math.random());
	       bank.getNode("py").setFloat(0, (float) Math.random());
	       bank.getNode("pz").setFloat(0, (float) Math.random());
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
