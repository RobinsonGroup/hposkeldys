package hpoutil.omim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorbidMapComplement {

	private Map<String, Map<String, List<String>>> assertions;
	
	public MorbidMapComplement(String complementFile) {
		assertions = new HashMap<String, Map<String,List<String>>>();
		
		if (complementFile != null) {
			loadFile(complementFile);
		}
	}

	private void loadFile(String complementFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(complementFile));
			
			while (br.ready()) {
				String line = br.readLine();
				addLineToMap(line);
			}
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void addLineToMap(String line) {
		String[] parts = line.split(" ");
		if (parts.length != 3) {
			System.out.println(" - ERROR: Line not propertly formatted: " + line);
			return;
		}
		
		String omim = parts[0].trim();
		String predicate = parts[1].trim();
		String gene = parts[2].trim();
		
		Map<String, List<String>> map = assertions.containsKey(omim) ? assertions.get(omim) : new HashMap<String, List<String>>();
		List<String> list = map.containsKey(predicate) ? map.get(predicate) : new ArrayList<String>();
		if (!list.contains(gene)) {
			list.add(gene);
		}
		map.put(predicate, list);
		assertions.put(omim, map);
	}
	
	public boolean hasAssertions(String omim) {
		return assertions.containsKey(omim);
	}
	
	public Map<String, List<String>> retrieveAssertions(String omim) {
		return assertions.get(omim);
	}
}
