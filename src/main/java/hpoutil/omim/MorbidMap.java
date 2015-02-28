package hpoutil.omim;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

//import de.charite.hpo.hpo2owl.util.Identifiers;

/**
 * A container for morbid map to get gene identifiers for a given MIM numbers.
 * 
 * @author Peter Robinson, Sebastian Bauer, Tudor Groza
 */
public class MorbidMap {

	private static Logger log = Logger.getLogger(MorbidMap.class.getName());
    /** Key: an OMIM id (phenotype), value: a list of the corresponding diseases. Note there may be multiple
     * entries of there is a susceptibility entry and an germline Mendelian entry (the OMIM data are mixed up)*/
    private HashMap<Integer, List<OMIMDisease>> disease2gene_map;


    public HashMap<Integer, List<OMIMDisease>>  getOMIMDiseaseMap() { return this.disease2gene_map; }
    
    public MorbidMap(String morbid_map_path) {
	disease2gene_map = new HashMap<Integer, List<OMIMDisease>>();
	parseMorbidMapFile(morbid_map_path);
	
	/*
	  System.out.println(" ========================== ");
	  for (String omim : disease2gene_map.keySet()) {
	  List<OMIMDisease> list = disease2gene_map.get(omim);
	  System.out.println(" - OMIM: " + omim);
	  for (OMIMDisease dis : list) {
	  System.out.println(" -- " + dis);
	  }
	  }
	*/
	log.info("Got a total of " + disease2gene_map.size() + " diseases from morbidmap");
	/*
	int one=0;
	int more=0;
	for (Integer id : disease2gene_map.keySet()) {
	    List<OMIMDisease> l = disease2gene_map.get(id);
	    if (l.size()==1) {
		one++;
	    } else if (l.size()>1) {
		System.out.println("MULTPLE ODs for id " + id);
		for (OMIMDisease od:l) {
		    System.out.println(od);
		}
		more++;
	    } else {
		System.out.println("No diseases found for id, major problem");
		System.exit(1);
	    }
	}
	System.out.println("one=" + one + ", more=" + more);
		System.exit(1);
	*/
    }

	/**
	 * Create the morbid map from the given file. A typical line looks as
	 * {@code Marfan syndrome, 154700 (3)|FBN1, MFS1, WMS2, SSKS, GPHYSD2, ACMICD|134797|15q21.1}
	 * with 154700 being the the phenotype MIM Number for the disease Marfan
	 * syndrome.
	 * 
	 * @param file
	 * @return the morbid map at your service
	 * @throws IOException
	 */
	private void parseMorbidMapFile(String morbid_map_path) {
	    log.trace("Processing Morbidmap file " + morbid_map_path);
	    try {
		FileInputStream fstream = new FileInputStream(morbid_map_path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		
		int c = 0;
		int unparsable = 0;
		
		String line;
		while ((line = br.readLine()) != null) {
		    OMIMDisease omim = new OMIMDisease(line);
		    if (omim.is_unparsable()) {
			unparsable++;
			continue; // SKip entry, could not parse it
		    }
		    
		    Integer omim_id = omim.getOMIM_ID();
		    if (disease2gene_map.containsKey(omim_id)) {
			checkAndAdd(omim);
		    } else {
			List<OMIMDisease> list = new ArrayList<OMIMDisease>();
			list.add(omim);
			disease2gene_map.put(omim_id, list);
		    }
		    //				disease2gene_map.put(omim_id, omim);
		    c++;
		}
		br.close();
		log.trace("Parsed " + c + " lines from OMIM Morbid Map");
		log.trace("Could not parse " + unparsable
			  + " lines from OMIM Morbid Map");
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    
    private void checkAndAdd(OMIMDisease omim) {
	List<OMIMDisease> list = disease2gene_map.get(omim.getOMIM_ID());
	int found = -1;
	for (int i = 0; i < list.size(); i++) {
	    OMIMDisease disease = list.get(i);
	    if (disease.sameAs(omim)) {
		found = i;
		break;
	    }
	}
	
	if (found != -1) {
	    OMIMDisease disease = list.get(found);
	    list.remove(found);
	    disease.merge(omim);
	    list.add(disease);
	    disease2gene_map.put(omim.getOMIM_ID(), list);
	} else {
	    list.add(omim);
	    disease2gene_map.put(omim.getOMIM_ID(), list);
	}
    }
    
    public boolean containsGeneInformation(String mim) {
	return this.disease2gene_map.containsKey(mim);
    }
    
    public Map<String, List<String>> getGenesInfo(String mim) {
	Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
	
	List<OMIMDisease> list = disease2gene_map.containsKey(mim) ? disease2gene_map.get(mim) : new ArrayList<OMIMDisease>();
	for (OMIMDisease disease : list) {
	    boolean found = false;
	    List<String> genes = disease.getGenes();
	    
	    if (disease.is_somatic()) {
		List<String> l = result.containsKey(Identifiers.HAS_SOMATIC_MUTATION_IN) ? result.get(Identifiers.HAS_SOMATIC_MUTATION_IN) : new ArrayList<String>();
		
		for (String gene : genes) {
		    if (!l.contains(gene)) {
			l.add(gene);
		    }
		}
		result.put(Identifiers.HAS_SOMATIC_MUTATION_IN, l);
		found = true;
	    }
	    if (disease.is_modifier()) {
		List<String> l = result.containsKey(Identifiers.HAS_MODIFYING_MUTATION_IN) ? result.get(Identifiers.HAS_MODIFYING_MUTATION_IN) : new ArrayList<String>();
		
		for (String gene : genes) {
		    if (!l.contains(gene)) {
			l.add(gene);
		    }
		}
		result.put(Identifiers.HAS_MODIFYING_MUTATION_IN, l);
		found = true;
	    }
	    
	    if (!found) {
		List<String> l = result.containsKey(Identifiers.HAS_GERMLINE_MUTATION_IN) ? result.get(Identifiers.HAS_GERMLINE_MUTATION_IN) : new ArrayList<String>();
		
		for (String gene : genes) {
		    if (!l.contains(gene)) {
			l.add(gene);
		    }
		}
		result.put(Identifiers.HAS_GERMLINE_MUTATION_IN, l);
	    }
	}
	
	return result;
    }
    /*
      public List<String> getGenes(String mim) {
      OMIMDisease d = this.disease2gene_map.get(mim);
      return (d == null) ? null : d.getGene();
      }
      
      public boolean is_somatic(String mim) {
      OMIMDisease d = this.disease2gene_map.get(mim);
      return (d == null) ? null : d.is_somatic();
      }
      
      public boolean is_modifier(String mim) {
      OMIMDisease d = this.disease2gene_map.get(mim);
      return (d == null) ? null : d.is_modifier();
      }
    */
    public static void main(String[] args) {
	MorbidMap morbidMap = new MorbidMap("/home/tudor/EXTRA_SPACE/NEW_Charite/morbidmap");
	System.out.println(morbidMap.getGenesInfo("109800"));
	//		System.out.println(morbidMap.containsGeneInformation("109800"));
	//		System.out.println(morbidMap.get_gene("109800"));
    }
}
