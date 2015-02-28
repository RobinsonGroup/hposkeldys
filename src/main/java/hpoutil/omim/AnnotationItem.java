package hpoutil.omim;

/** Represents one line of an annotation */

public class AnnotationItem {
    /** Representation of the HPO id, e.g., 1234 for HP:0001234 */
    public Integer hpo_id = null;
    /** Was this annotation a NOT annotation? */
    public boolean is_negated = false;
    /** An integer constant that represents the age of onset of the phenotypic abnormality.*/
    private int age_class = DEFAULT_AGE_OF_ONSET;
    
    public static final int CONGENITAL = 1;
    public static final int INFANTILE = 2;
    public static final int NEONATAL = 3;
    public static final int CHILDHOOD = 4;
    public static final int ADULT = 5;
    public static final int DEFAULT_AGE_OF_ONSET = 77;
    
    
    public AnnotationItem(String hpoid, boolean negated) {
	this.hpo_id = parseHpoID(hpoid);
	this.is_negated = negated;
    }

    public boolean is_congenital() {
	return age_class == CONGENITAL;
    }
    
    public boolean is_neonatal() {
	return age_class == NEONATAL;
    }
    
    public boolean is_infantile() {
	return age_class == INFANTILE;
    }
    
    public boolean is_childhood() {
	return age_class == CHILDHOOD;
    }


    public Integer getHPOid() { return this.hpo_id;}


    public boolean is_negated() { return this.is_negated; }
    
    public boolean is_adult() {
	return age_class == ADULT;
    }
    
    public void set_congenital_age() {
	age_class = CONGENITAL;
    }
    
    public void set_neonatal_age() {
	age_class = NEONATAL;
    }
    
    public void set_infantile_age() {
	age_class = INFANTILE;
    }
    
    public void set_childhood_age() {
	age_class = CHILDHOOD;
    }
    
    public void set_adult_age() {
	age_class = ADULT;
    }


     /**
     * @param s A string such as HP:0001234
     * @return the corresponding Integer value (e.g., 1234)
     * @throws IllegalArgumentException for invalid HPO string
     */
    public Integer parseHpoID(String hpid) {
	String s=hpid;
	if (s.startsWith("HP:"))
	    s=s.substring(3).trim();
	else {
	    throw new IllegalArgumentException("Malformed HP id: \""+hpid + "\"");
	}
	if (s.length()!=7) {
	    throw new IllegalArgumentException("Malformed HP id: \""+hpid+ "\" (length=" + s.length() + ")");
	}
	try {
	    Integer ii = Integer.parseInt(s);
	    return ii;
	} catch (NumberFormatException e) {
	    throw new IllegalArgumentException("Malformed HP id:"+e.getMessage());
	}
    }

    
}
/* eof */
