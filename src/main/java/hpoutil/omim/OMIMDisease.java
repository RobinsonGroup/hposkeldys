package hpoutil.omim;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a single line in the OMIM Morbid map file Essential
 * pieces of information: 1) Gene, 2) Disease Name 3) Somatic or Germline
 * mutation
 * 
 * @author peter.robinson@charite.de
 */

public class OMIMDisease {

    private ArrayList<String> genes;
    
    /** Integer representation of the OMIM id, e.g., for OMIM:123456, store 123456 as an Integer. */
    private Integer omim_id = null;

    /** Is this entry for a somatic mutation? */
    private boolean is_somatic = false;
    
    /** Is this entry a modifying gene? */
    private boolean is_modifier = false;
    
    /**
     * Did we have a problem parsing this line? (Will be used as a flag to skip
     * this entry)
     */
    private boolean is_unparsable = false;
    
    public OMIMDisease(String line) {
	this.genes = new ArrayList<String>();
	parse_line(line);
    }
    
    private void parse_line(String line) {
	String[] segments = line.split("\\|");
	
	String nameField = segments[0];
	String geneField = segments[1];
	
	// The name field contains, somewhere, the MIM number.
	// Look for six digits following one another
	boolean previousCharIsDigit = false;
	int n_digits = 0;
	int len = nameField.length();
	int pos = 0;
	while (pos < len) {
	    if (Character.isDigit(nameField.charAt(pos))) {
		if (previousCharIsDigit) {
		    n_digits++;
		    if (n_digits == 6) {
			String omim = nameField.substring(pos - 5, pos + 1);
			this.omim_id=Integer.parseInt(omim); 
			break;
		    }
		} else {
		    n_digits = 1;
		    previousCharIsDigit = true;
		}
	    } else {
		n_digits = 0;
		previousCharIsDigit = false;
	    }
	    pos++; // current String index.
	}
	
	if (this.omim_id == null) {
	    this.is_unparsable = true;
	    return; // Skip rest of entry...
	}
	
	/* For somatic mutations, the nameField contains the word "Somatic" */
	int i = nameField.indexOf("somatic");
	if (i >= 0) {
	    this.is_somatic = true;
	}
	
	i = nameField.indexOf("modifier");
	int j = nameField.indexOf("modification");
	if (i >= 0 || j >= 0) {
	    this.is_modifier = true;
	}
	
	String geneSegments[] = geneField.split(",");
	for (String geneSegment : geneSegments) {
	    geneSegment = geneSegment.trim();
	    String geneSegments2[] = geneSegment.split(";");
	    for(String gene : geneSegments2) {
		gene = gene.trim();
		String geneSegments3[] = gene.split("\\.");
		for(String geneG : geneSegments3) {
		    geneG = geneG.trim();
		    genes.add(geneG);
		}
	    }
	}
	
	if (genes.size() == 0) {
	    System.out.println("Could not parse morbid map line (gene): + \n\t" + line);
	    is_unparsable = true;
	    return;
	}
	/*
	// Now get gene name or disease symbol. Just take the first symbol,
	// we are not interested in all the synonyms for now.
	String geneSegments[] = geneField.split(",");
	this.gene = geneSegments[0];
	int ix = gene.indexOf(";"); // Deal with names such as ABCD2;HF23
	// (Protege cannot deal).
	if (ix > 0) {
	gene = gene.substring(0, ix);
	}
	if (gene == null) {
	System.out.println("Could not parse morbid map line (gene): + \n\t"
	+ line);
	is_unparsable = true;
	return;
	}
	
	*/
    }
    
    public boolean sameAs(OMIMDisease disease) {
	boolean same = false;
	if (is_somatic) {
	    if (!disease.is_somatic()) {
		same = false;
	    } else {
		same = true;
	    }
	} else {
	    if (!disease.is_somatic()) {
		same = true;
	    } else {
		same = false;
	    }
	}
	
	if (same) {
	    if (is_modifier) {
		if (disease.is_modifier()) {
		    same = true;
		} else {
		    same = false;
		}
	    } else {
		if (disease.is_modifier()) {
		    same = false;
		} else {
		    same = true;
		}
	    }
	}
	
	return same;
    }
    
    public ArrayList<String> getGenes() {
	return this.genes;
    }
    
    public Integer getOMIM_ID() {
	return this.omim_id;
    }
    
    public boolean is_somatic() {
	return this.is_somatic;
    }
    
    public boolean is_modifier() {
	return is_modifier;
    }
    
    public boolean is_unparsable() {
	return is_unparsable;
    }
    
    public void merge(OMIMDisease omim) {
	for (String gene: omim.getGenes()) {
	    if (!genes.contains(gene)) {
		genes.add(gene);
	    }
	}
    }
    
    @Override
    public String toString() {
	return " - " + omim_id  + " - " + is_somatic + " - " + is_modifier + " - " + genes;
    }
}

/* eof */
