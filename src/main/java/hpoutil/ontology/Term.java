package hpoutil.ontology;


import java.util.ArrayList;


public class Term {

    private String name=null;
    private Integer id;
    private ArrayList<Integer> isa;

    private ArrayList<Term> parentList=new ArrayList<Term>();

    private ArrayList<Term> childList=new ArrayList<Term>();

    public Term(String current_name, String current_id, ArrayList<String> current_isa) {
	this.id = parseHpoID(current_id);
	this.name=current_name;
	isa=new ArrayList<Integer>();
	for (String s:current_isa) {
	    // The "is_a" String may have the form
	    // HP:0011628 ! Congenital defect of the pericardium
	    int i = s.indexOf("!");
	    if (i>0)
		s=s.substring(0,i);
	    Integer id = parseHpoID(s);
	    isa.add(id);
	}
	//System.out.println(toString());
    }



    public Integer getID() { return this.id; }

    public String getName() { return this.name; }

    public ArrayList<Integer> getParentIDs() { return this.isa; }

    public void addIsAReference(Term parent) {
	this.parentList.add(parent);
    }

    public void addChildReference(Term child) {
	this.childList.add(child);
    }


    private String IdToString(Integer hpoid) {
	return String.format("HP:%07d",hpoid);
    }
    
    public String toString() {
	StringBuffer sb = new StringBuffer();
	for (Integer i:this.isa) {
	    sb.append(IdToString(i) + "; ");
	}
	return String.format("%s [%s] is_a: %s",this.name,IdToString(this.id),sb.toString());
    }


    /**
     * @param s A string such as HP:0001234
     * @return the corresponding Integer value (e.g., 1234)
     * @throws IllegalArgumentException for invalid HPO string
     */
    public Integer parseHpoID(String s) {
	if (s.startsWith("HP:"))
	    s=s.substring(3).trim();
	else {
	    throw new IllegalArgumentException("Malformed HP id:"+s);
	}
	if (s.length()!=7) {
	    throw new IllegalArgumentException("Malformed HP id:"+s+ " (length=" + s.length() + ")");
	}
	try {
	    Integer ii = Integer.parseInt(s);
	    return ii;
	} catch (NumberFormatException e) {
	    throw new IllegalArgumentException("Malformed HP id:"+e.getMessage());
	}
    }




}
