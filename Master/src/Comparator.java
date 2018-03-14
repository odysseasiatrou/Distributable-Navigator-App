
public class Comparator {
	
	public double euclidean_metric(String s1, String s2){
		
		String lat1 = s1.substring(0, s1.indexOf(",")).replace(",","");
        String lon1 = s1.substring(s1.indexOf(",")).replace(",", "");
		String lat2 = s2.substring(0, s2.indexOf(",")).replace(",","");
		String lon2 = s2.substring(s2.indexOf(",")).replace(",","");
		
		double dif1 = Math.abs(Integer.parseInt(lat1)-Integer.parseInt(lat2));
		double dif2 = Math.abs(Integer.parseInt(lon1)-Integer.parseInt(lon2));
		
		double euclidean_equation = Math.sqrt((dif1*dif1)+(dif2*dif2));
		return euclidean_equation;
	}
}
