package hpoutil.io;



import java.io.*;
import java.util.ArrayList;

import hpoutil.ontology.*;

/**
 * Parse the OBO version of the HPO
 * and create one Term object for each 
 * [Term]
 * @author Peter Robinson
 * @version 0.1
 */
public class HPOParser {

    private ArrayList<Term> termlist=null;

    private String current_id=null;
    private String current_name=null;
    private ArrayList<String> current_isa=null;


    public HPOParser(String path) {
	termlist=new ArrayList<Term>();
	parseFile(path);

    }

    public ArrayList<Term> getTermList() { return this.termlist; }

    private void parseFile(String path) {
	try {
	    FileInputStream fstream = new FileInputStream(path);
	    DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    String line;
	    current_isa=new ArrayList<String>();
	    while ((line = br.readLine()) != null)   {
		if (line.startsWith("[Term]")) {
		    // Check if we have all infos for a term
		    if (current_id!=null && current_name!=null) {
			Term t = new Term(current_name, current_id,current_isa);
			termlist.add(t);
			current_id=null;
			current_name=null;
			current_isa.clear();
		    }
		} else if (line.startsWith("id:")) {
		    current_id=line.substring(4).trim();
		} else if (line.startsWith("name:")) {
		    current_name=line.substring(6).trim();
		} else if (line.startsWith("is_a:")) {
		    String ia = line.substring(6).trim();
		    current_isa.add(ia);
		}
	    }
	    /* also get the very last term*/
	    if (current_id!=null && current_name!=null) {
		Term t = new Term(current_name, current_id,current_isa);
		termlist.add(t);
	    }
	    in.close();
	} catch (IOException e) {
	    e.printStackTrace();

	}



    }



}
