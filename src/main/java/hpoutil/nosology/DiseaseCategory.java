package hpoutil.nosology;

import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.HashMap;
import java.io.Writer;
import java.io.IOException;

import hpoutil.ontology.*;
import hpoutil.omim.DiseaseAnnotation;

/**
 * This class represents one of the 40 skeletal nosology categories
 * @author Peter Robinson
 * @version 0.01 (25 Feb)
 */
public class DiseaseCategory {

    private static Logger log = Logger.getLogger(DiseaseCategory.class.getName());

    private String categoryname=null;
    /** reference to teh HPO object */
    private static HPO hpo;

    private ArrayList<String> diseasegenes=null;
    private ArrayList<Integer> featurelist=null;
    private ArrayList<Integer>notFeaturelist=null;

    HashMap<Integer,String> goldstandard=null;
    /** Count of diseases we evaluated but found not to be members of this category */
    int notMemberCount=0;

    ArrayList<DiseaseAnnotation> goodCandidate=new ArrayList<DiseaseAnnotation>();

    public void addGoldStandard(HashMap<Integer,String> gs) { this.goldstandard=gs; }

    public DiseaseCategory( String name, ArrayList<String> diseasegenes,ArrayList<Integer> flist,ArrayList<Integer>notflist) {
	this.categoryname=name;
	this.diseasegenes=diseasegenes;
	this.featurelist=flist;
	this.notFeaturelist=notflist;
    }

    static public void setHPO(HPO hpo) {
	DiseaseCategory.hpo=hpo;
    }



    private String join(String conn, ArrayList<String> lst) {
	StringBuilder sb = new StringBuilder();
	sb.append(lst.get(0));
	for (int i=1; i<lst.size(); ++i) {
	    sb.append(conn + lst.get(i));
	}
	return sb.toString();
    }

    private String joinHPO(ArrayList<Integer> lst) {
	if (lst==null || lst.size()==0)
	    return "No constraint";
	else if (lst.size()==1) {
	    Integer id = lst.get(0);
	    String name = this.hpo.getTermName(id);
	    return String.format("HP:%07d: %s",id,name); 
	} else {
	    StringBuilder sb = new StringBuilder();
	    boolean notfirst=false;
	    for (Integer id : lst) {
		String name = this.hpo.getTermName(id);
		String s =String.format("HP:%07d: %s",id,name); 
		if (notfirst) sb.append("; ");
		sb.append(s);
		notfirst=true;
	    }
	    return sb.toString();
	}
    }



    public void printDefinition(Writer out) throws IOException {
	int n = this.diseasegenes.size();
	out.write("Disease Gene: ");
	switch(n) {
	case 0: out.write("no constraint.\n"); break;
	case 1: out.write(this.diseasegenes.get(0) + ".\n"); break;
	default: out.write(join(";",this.diseasegenes) + "\n");break;
	}
	out.write("NOT features: " + joinHPO(this.notFeaturelist) + "\n");
	out.write("Required features: " + joinHPO(this.featurelist)+ "\n");
    }



    public void printOutput(Writer out) throws IOException {
	ArrayList<String> newprediction = new ArrayList<String>();
	int n_gc_identified=0;
	HashMap<Integer,Boolean> foundGS=new HashMap<Integer,Boolean>();
	for (Integer id : this.goldstandard.keySet()) {
	    foundGS.put(id,false);
	}
	out.write(this.categoryname + "\n");
	printDefinition(out);
	out.write("Predictions:\n");
	for (DiseaseAnnotation da : goodCandidate) {
	    Integer id = da.getDiseaseId();
	    if (goldstandard.containsKey(id)) {
		n_gc_identified++;
		out.write(n_gc_identified + ") " + da.toString() + "[+]\n");
		foundGS.put(id,true);
	    } else {
		newprediction.add(da.toString());
	    }
	}
	int tot = goldstandard.size();
	out.write(String.format("I got %d/%d (%.1f%%) of the gold standard diseases.\n",n_gc_identified,tot,((double)100*n_gc_identified/tot)));
	if (n_gc_identified < tot) {
	    for (Integer id : foundGS.keySet()) {
		if (foundGS.get(id))
		    continue;
		else {
		    String mimname = goldstandard.get(id);
		    if (mimname==null) mimname="?";
		    String ret = String.format("MIM:%06d %s",id,mimname);
		    out.write("Did not found disease " + ret + "\n");

		}
	    }
	}
	if (newprediction.size()==0) {
	    out.write("No new predictions\n");
	} else {
	    out.write("New predictions:\n");
	    for (String s: newprediction) {
		out.write("\t"+s+"\n");
	    }
	}


    }



    public String getName() { return this.categoryname; }


    public void findMembers(HashMap<Integer,DiseaseAnnotation> diseasemap) {
	for (Integer id:diseasemap.keySet()) {
	    DiseaseAnnotation d = diseasemap.get(id);
	    boolean ok = evaluateCandidateDisease(d);
	    if (ok) {
		goodCandidate.add(d);
		System.out.println("Found candidate " + d);
	    }
	}
    }


    public boolean evaluateCandidateDisease(DiseaseAnnotation disease) {
	boolean verbose = false;
	if (disease.MIMid().equals(156530))
	    verbose=true;
	if (this.diseasegenes != null && this.diseasegenes.size()>0) {
	    boolean match=false; // at least one gene must match
	    for (String symbol:this.diseasegenes) {
		if (disease.hasDiseaseGene(symbol)) {
		    match=true;
		    break;
		}
	    }
	    if (!match) {
		notMemberCount++;
		return false;
	    }
	}
	// If a disease has a NOT feature, then it cannot belong to this category
	boolean reject = false;
	for (Integer not: notFeaturelist) {
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	     for (Integer pos:positiveannotations) {
		 try{
		     if (DiseaseCategory.hpo.isAncestorOf(not,pos,verbose)){
			 notMemberCount++;
			 return false;
		     }
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	 }
	if (this.featurelist==null || this.featurelist.size()==0) {
	    /* If we get here, then the DiseaseCategory definition does
	       not have any HPO terms that a candidate disease has to have, 
	       and the other conditions about disease genes and NOT terms
	       were ok */
	    return true;
	}
	int n_found=0;
	for (Integer yes: this.featurelist) {
	    if (verbose) {
		String name = this.hpo.getTermName(yes);
		System.out.println("featurelist:"+yes + ":" + name);
	    }
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	     for (Integer pos:positiveannotations) {
		 if (verbose) {
		     String name = this.hpo.getTermName(pos);
		     System.out.println("\t Test annot:"+pos + ":" + name);

		 }
		 try{
		     if (DiseaseCategory.hpo.isAncestorOf(yes,pos,verbose)){
			 if (verbose) System.out.println("\tIS ANC");
			n_found++;
			break;
		     } else {
			 if (verbose) System.out.println("\tNOT ANC");
		     }
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	}
	if (n_found==this.featurelist.size())
	    return true;
	else
	    return false;

    }
    


}
