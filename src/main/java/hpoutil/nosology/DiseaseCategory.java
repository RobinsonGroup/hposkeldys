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
    /** At least one of these features must be present */
    private ArrayList<Integer> optionalFeaturelist=null;
    /** At least {@link #N} descendents of this feature must be present
	to call the disease*/
    private ArrayList<Integer> featureNlist;
    /** Minimum number of times {@link #featureNlist} must be present
     Note: Order of arraylist matches.*/
    private ArrayList<Integer> N;

    HashMap<Integer,String> goldstandard=null;
    /** Count of diseases we evaluated but found not to be members of this category */
    int notMemberCount=0;

    ArrayList<DiseaseAnnotation> goodCandidate=new ArrayList<DiseaseAnnotation>();

    public void addGoldStandard(HashMap<Integer,String> gs) { this.goldstandard=gs; }

    public void setOptionalList(ArrayList<Integer> list) {
	optionalFeaturelist=list;
    }
    /** Sets HPO terms that must be present at least N times. Note, 
	featureNlist.get(i) must be present N.get(i) times.
    */
    public void setFeatureN(ArrayList<Integer> featureNlist,ArrayList<Integer> N) {
	this.featureNlist=featureNlist;
	this.N=N;
    }

    public DiseaseCategory( String name, ArrayList<String> diseasegenes,ArrayList<Integer> flist,ArrayList<Integer>notflist) {
	this.categoryname=name;
	this.diseasegenes=diseasegenes;
	this.featurelist=flist;
	this.notFeaturelist=notflist;
    }

    static public void setHPO(HPO hpo) {
	DiseaseCategory.hpo=hpo;
    }

    public ArrayList<Integer> getMemberList() {
	ArrayList<Integer> l = new ArrayList<Integer>();
	for (DiseaseAnnotation da:this.goodCandidate) {
	    Integer id = da.MIMid();
	    l.add(id);
	}
	return l;
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
	    String name = DiseaseCategory.hpo.getTermName(id);
	    return String.format("HP:%07d: %s",id,name); 
	} else {
	    StringBuilder sb = new StringBuilder();
	    boolean notfirst=false;
	    for (Integer id : lst) {
		String name = DiseaseCategory.hpo.getTermName(id);
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
	out.write("Optional features(>=1 must be present):" + joinHPO(this.optionalFeaturelist)+"\n");
	if (this.featureNlist!=null) {
	    int len = this.featureNlist.size();
	    for (int i=0;i<len;++i) {
		Integer id = this.featureNlist.get(i);
		n = this.N.get(i);
		String name = DiseaseCategory.hpo.getTermName(id);
		String s =String.format("HP:%07d: %s",id,name); 
		out.write(String.format("Require at least %d descendents of %s\n",n,s));
	    } 
	}
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
	    out.write(String.format("New predictions (n=%d):\n",newprediction.size()));
	    int i=0;
	    for (String s: newprediction) {
		out.write("\t" + (++i) + ": " +s+"\n");
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
	    }
	}
    }


    public boolean evaluateCandidateDisease(DiseaseAnnotation disease) {
	boolean verbose=false;
	if (disease.MIMid().equals(200600))
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
		     if (DiseaseCategory.hpo.isAncestorOf(not,pos)){
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

	if (featureNlist!=null && featureNlist.size()>0) {
	    int i;
	    int len = featureNlist.size();
	    for (i=0;i<len;i++) {
		Integer feat = featureNlist.get(i);
		Integer n = N.get(i);
		//String name = DiseaseCategory.hpo.getTermName(feat);
		//String s =String.format("HP:%07d: %s",feat,name); 
		//System.out.println("Looking for at least " + n + " annotations for " + s + " in " + disease.getDiseaseName());
		ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
		int n_found=0;
		for (Integer pos:positiveannotations) {
		    try{
			if (DiseaseCategory.hpo.isAncestorOf(feat,pos)){
			    n_found++;
			} 
			
		    } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
		}
		if (n_found < n)
		    return false;
	    }
	}


	int n_found=0;
	for (Integer yes: this.featurelist) {
	    //if (verbose) {
	    //	System.out.println(String.format("-required- HP:%07d - %s",yes,DiseaseCategory.hpo.getTermName(yes)));
	    // }
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	    for (Integer pos:positiveannotations) {
		//	if (verbose) {
		//  System.out.println(String.format("-annot-HP:%07d - %s",pos,DiseaseCategory.hpo.getTermName(pos)));
		//}
		 try{
		     if (DiseaseCategory.hpo.isAncestorOf(yes,pos)){
			 n_found++;
			 if (verbose)
			     System.out.println("FOUND:" + pos);
			break;
		     } 
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	}
	if (n_found<this.featurelist.size())
	    return false;
	if (this.optionalFeaturelist==null || this.optionalFeaturelist.size()==0)
	    return true;
	// If we get here, then just one of the optional features must be present
	for (Integer optf : this.optionalFeaturelist) {
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	    for (Integer pos:positiveannotations) {
		 try{
		     if (DiseaseCategory.hpo.isAncestorOf(optf,pos)){
			 return true;
		     } 
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	}
	return false; // we did not find an optional annotation.

    }
    


}
