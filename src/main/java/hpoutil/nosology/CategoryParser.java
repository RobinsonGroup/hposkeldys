package hpoutil.nosology;



import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;


public class CategoryParser {

    private final static String nosopath="src/main/resources/nosology";

    private ArrayList<DiseaseCategory> categorylist=null;

   
    public ArrayList<DiseaseCategory> getDiseaseCategoryList() { return this.categorylist; }

    public CategoryParser() {
	ArrayList<String> paths=getFilePaths();

	this.categorylist = new ArrayList<DiseaseCategory> ();
	for (String p : paths) {   
	    parseCategory(p);
	}
    }






    /*
      name:FGFR3Group
      gold:187600=Thanatophoric dysplasia type 1 (TD1)
      gold:187601=Thanatophoric dysplasia type 2 (TD2),Severe achondroplasia with developmental delay and acanthosis nigricans (SADDAN)
      gold:100800=Achondroplasia
      gold:146000=Hypochondroplasia
      gold: 610474=Camptodactyly, tall stature, and hearing loss syndrome (CATSHL)
      //[187600, 187601, 100800, 146000, 610474]
      hasGermlineMutationIn:FGFR3
      notHasFeature:HP_0001363
      // NOT HPO_craniosynostosis (HP:0001363)
      // NOT Triphalangeal thumb (HP:0001199)
      //!#hasFeature HP_0001363
      //!#hasFeature HP_0001199
      */

    private void parseCategory(String filepath) {

	HashMap<Integer,String> goldstandard=new  HashMap<Integer,String>();
	ArrayList<String> diseasegenes=new ArrayList<String>();
	ArrayList<Integer> featurelist=new ArrayList<Integer>();
	ArrayList<Integer> notFeaturelist=new ArrayList<Integer>();
	String name=null;
	try {
	    BufferedReader br = new BufferedReader(new FileReader(filepath));
	    String line=null;
	    while ((line = br.readLine())!=null) {
		if (line.startsWith("//"))
		    continue;
		else if (line.isEmpty())
		    continue;
		else if (line.startsWith("name:"))
		    name=line.substring(5).trim();
		else if (line.startsWith("gold:")) {
		    String gold = line.substring(5).trim();
		    String a[] = gold.split("=");
		    Integer mim = Integer.parseInt(a[0].trim());
		    goldstandard.put(mim,a[1].trim());
		} else if (line.startsWith("hasGermlineMutationIn:")) {
		    int i = line.indexOf(":");
		    String sym = line.substring(i+1).trim();
		    diseasegenes.add(sym);
		} else if (line.startsWith("notHasFeature:")) {
		    String hp=line.substring(14).trim();
		    Integer hpo=getHPcode(hp);
		    notFeaturelist.add(hpo);
		} else if (line.startsWith("hasFeature:")) {
		    String hp=line.substring(11).trim();
		    Integer hpo=getHPcode(hp);
		    featurelist.add(hpo);
		}
	    }
	    br.close();
	} catch(IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	DiseaseCategory dc = new DiseaseCategory(name,diseasegenes,featurelist,notFeaturelist);
	dc.addGoldStandard(goldstandard);
	this.categorylist.add(dc);
	    
    }


	private Integer getHPcode(String h) {
	    if (h.startsWith("HP_") || h.startsWith("HP:"))
		h = h.substring(3).trim();
	    if (h.length()!=7)
		throw new IllegalArgumentException("Bad length for HP term in CategoryParser:" + h);
	    return Integer.parseInt(h);
	}



    private ArrayList<String> getFilePaths() {
	ArrayList<String> paths = new ArrayList<String>();
	File folder = new File(nosopath);
	if (folder.exists()) {
	    File[] files = folder.listFiles();
	    for (File file : files) {
		if (file.isFile()) {
		    if (file.getName().endsWith("nos")) {
			paths.add(file.getAbsolutePath());
		    }
		} 
	    }
	    //log.info("Compiled a total of " + paths.size()+ " annotation files.");
	} else {
	    System.out.println("Could not initialize annotation folder: " + nosopath);
	    System.out.println("Please check path (e.g., in makefile variable NOSOLOGY) and try again");
	    System.exit(1);
	}
	return paths;


    }



}
