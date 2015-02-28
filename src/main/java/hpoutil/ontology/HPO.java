package hpoutil.ontology;



import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represent the HPO ontology with the Terms and
 * the is_a links
 * @author Peter Robinson
 * @version 0.1 (24 Feb, 2015)
 */
public class HPO {


    private HashMap<Integer,Term> termmap=null;

    private Term phenoRoot=null;

    private Term onsetRoot=null;

    private Term inheritanceRoot=null;

    public HPO(ArrayList<Term> tlist) {
	calculateIsaAncestry(tlist);
	identifyRootTerms();
    }

    public String getTermName(Integer i) {
	Term t = this.termmap.get(i);
	if (t==null) return "?";
	else return t.getName();
    }


    /**
     * This method looks for the terms
     * HP:0000118 (Phenotypic abnormality) and
     * HP:0000005 (Mode of inheritance) and
     * HP:0003674 (Onset)
     */
    private void identifyRootTerms() {
	Term pheno = this.termmap.get(118);
	if (pheno==null) {
	    System.err.println("[HPO.java: ERROR] Could not identifiy phenotypic abnormality term");
	    System.exit(1);
	} else {
	    this.phenoRoot=pheno;
	}
	Term onset = this.termmap.get(3674);
	if (onset==null) {
	    System.err.println("[HPO.java: ERROR] Could not identify ONSET term");
	    System.exit(1);
	} else {
	    this.onsetRoot=pheno;
	}
	Term inherit = this.termmap.get(5);
	if (inherit==null) {
	    System.err.println("[HPO.java: ERROR] Could not identify INHERITANCE term");
	    System.exit(1);
	} else {
	    this.inheritanceRoot=pheno;
	}
	System.err.println("[INFO] Created HPO Ontology successfully");
    }


    /**
     * return true of t1 is an ansecetor of t2
     */
    public boolean isAncestorOf(Integer t1, Integer t2,boolean verbose) {
	Term term1 = this.termmap.get(t1);
	Term term2 = this.termmap.get(t2);
	if (term1==null) {
	    System.err.println("[HPO.java ERROR] could not find term for t1=" + t1);
	    throw new IllegalArgumentException("[HPO.java ERROR] could not find term for t1=" + t1);
	}
	if (term2==null) {
	    System.err.println("[HPO.java ERROR] could not find term for t2=" + t2);
	     throw new IllegalArgumentException("[HPO.java ERROR] could not find term for t2=" + t2);
	   
	}
	if (t1.equals(t2))
	    return true;
	java.util.Stack<Term> candidates = new java.util.Stack<Term>();
	candidates.push(term2);
	while (! candidates.empty() ) {
	    Term t = candidates.pop();
	    if (verbose) {
		System.out.println("Popped term " +t.getName() + " t1=" + term1.getName());
	    }
	    ArrayList<Term> parents = t.getParents();
	    for (Term p : parents) {
		if (p.equals(term1))
		    return true;
		if (p.equals(phenoRoot))
		    continue; /* end of the ontology */
		candidates.push(p);
	    }
	}
	return false;
    }





    private void calculateIsaAncestry(ArrayList<Term> tlist){
	this.termmap = new HashMap<Integer,Term>();
	for (Term t: tlist)  {
	    Integer id = t.getID();
	    this.termmap.put(id,t);
	}
	for (Term t: tlist) {
	    Integer id = t.getID();
	    ArrayList<Integer> parentIDlist = t.getParentIDs();
	    for (Integer p : parentIDlist) {
		Term parent = this.termmap.get(p);
		if (parent==null) {
		    System.err.println("Error, could not find term for parent id: " + p);
		    System.exit(1);
		}
		t.addIsAReference(parent);
		parent.addChildReference(t);
	    }
	}


    }


}
