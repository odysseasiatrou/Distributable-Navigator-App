import java.io.Serializable;
import java.util.ArrayList;

public class DirectionsObject implements Serializable {

	private String startLatLon;
	private String endLatLon;
	private String json_string;
	private ArrayList<String> jsonList;
	
	public ArrayList<String> getJsonList() {
		return jsonList;
	}
	public void setJsonList(ArrayList<String> jsonList) {
		this.jsonList = jsonList;
	}
	public String getJson_string() {
		return json_string;
	}
	public void setJson_string(String json_string) {
		this.json_string = json_string;
	}
	
	public String getStartLatLon() {
		return startLatLon;
	}
	public void setStartLatLon(String startLatLon) {
		this.startLatLon = startLatLon;
	}
	
	public String getEndLatLon() {
		return endLatLon;
	}
	public void setEndLatLon(String endLatLon) {
		this.endLatLon = endLatLon;
	}
	
	public String getFromList(int index) {
		return jsonList.get(index);
	}
	public void addJSON(String jString) {
		this.jsonList.add(jString);
	}
	
}
