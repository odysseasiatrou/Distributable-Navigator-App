import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

public class Mapper {

	private static ServerSocket  server_socket;
	private static ArrayList<DirectionsObject> directionsObject_list;
	private static final String filename = "mapperFile2.txt";
	private static BufferedReader streamer; 

	public static void main(String[] args) {
		initialize();
	}
	
	private static void initialize(){
		
		server_socket = null;
		Socket connection = null;
		directionsObject_list = new ArrayList<DirectionsObject>();
		ArrayList<DirectionsObject> directionsList = new ArrayList<DirectionsObject>(); 
		
		
		try{
			
			DirectionsObject dir = null;
			FileReader file = new FileReader(filename);
			BufferedReader streamer = new BufferedReader(file);
			String line = null, endLatLon = "", startLatLon = "", directionsObject = "";
			boolean flag = true;
			ArrayList<DirectionsObject> directions_list = new ArrayList<DirectionsObject>();
			
			while((line = streamer.readLine())!=null){
				if(line.indexOf("Response")>=0){
					if(dir!=null){
						dir.setJson_string(directionsObject);
						dir.addJSON(directionsObject);
						directionsObject = "";
						directions_list.add(dir);
						line = streamer.readLine();
					}
					dir =new DirectionsObject();
					flag = true;
				}
				if(line.indexOf("end_location")>=0&&flag){
					directionsObject += line+"\n";
					String[] s = streamer.readLine().replaceAll("\\s+", "").split(":");
					endLatLon += s[1];
					directionsObject += line+"\n";
					s = streamer.readLine().replaceAll("\\s+", "").split(":");
					endLatLon += s[1];
					dir.setEndLatLon(endLatLon);
					endLatLon = "";
				}
				else if(line.indexOf("start_location")>=0&&flag){
					directionsObject += line+"\n";
					String[] s = streamer.readLine().replaceAll("\\s+", "").split(":"); 
					startLatLon += s[1];
					directionsObject += line+"\n";
					s = streamer.readLine().replaceAll("\\s+", "").split(":");
					startLatLon += s[1];
					dir.setStartLatLon(startLatLon);
					startLatLon = "";
				}
				if(line.indexOf("steps")>=0)
					flag = false;
				directionsObject += line+"\n";
			}		
			
			//also add the final recording
			dir.setJson_string(directionsObject);
			dir.addJSON(directionsObject);
			directionsObject = "";
			directions_list.add(dir);
			line = streamer.readLine();	
			directionsObject_list.addAll(directions_list);
			
			
		}catch(IOException e){
			e.printStackTrace();
		}
		
		try{
			server_socket = new ServerSocket(4321);
			String message = "";
			while(true){
				System.out.println("waiting for connection ...");
				connection = server_socket.accept();
				
				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

				message = "Connection succesfull !";
				out.writeObject(message);
				out.flush();
				message = (String) in.readObject();
				if(message.equals("want some new work from you")){
					message = (String) in.readObject();
					if(message.equals("search to your local file")){
						String srcLatLng=(String) in.readObject();
	                    String dstLatLng=(String) in.readObject();
	                    
	                    //separate latitude, longtitude coordinates
	                    String srcLat = srcLatLng.substring(0, srcLatLng.indexOf(","));
	                    String srcLon = srcLatLng.substring(srcLatLng.indexOf(",")).replace(",","");
	            		String dstLat = dstLatLng.substring(0, dstLatLng.indexOf(","));
	            		String dstLon = dstLatLng.substring(dstLatLng.indexOf(",")).replace(",","");
	            		
	            		//make the coordinates double precision
	                    srcLat = srcLat.substring(0, srcLat.indexOf(".")+3);
	                    srcLon = srcLon.substring(0, srcLon.indexOf(".")+3);
	                    dstLat = dstLat.substring(0, dstLat.indexOf(".")+3);
	                    dstLon = dstLon.substring(0, dstLon.indexOf(".")+3);
	                     
	                    final String sourceLat = srcLat;
	                    final String sourceLon = srcLon;
	                    final String destinationLat = dstLat;
	                    final String destinationLon = dstLon;
	                    
	                    //map source lat lng
	                    ArrayList<ArrayList<DirectionsObject>> list = new ArrayList<ArrayList<DirectionsObject>>();
	                    list.add(directionsObject_list);
	                    for(DirectionsObject dir : directionsObject_list)
	                    	System.out.println(dir.getStartLatLon());
	                    List<Map<String, DirectionsObject>> mappedDestination = list.stream().map(p -> {
	                        Map<String,DirectionsObject> map = new HashMap<String,DirectionsObject>();
	                         
	                        for(DirectionsObject dir: p){

	                            //separate latitude, longtitude coordinates
	                            String sLat = dir.getStartLatLon().substring(0, dir.getStartLatLon().indexOf(","));
	                            String sLon = dir.getStartLatLon().substring(dir.getStartLatLon().indexOf(",")).replace(",","");
	                            String dLat = dir.getEndLatLon().substring(0,dir.getEndLatLon().indexOf(","));
	                            String dLon = dir.getEndLatLon().substring(dir.getEndLatLon().indexOf(",")).replace(",","");
	                            
	                            //make the coordinates double precision
	                            sLat = sLat.substring(0, sLat.indexOf(".")+3);
	                            sLon = sLon.substring(0, sLon.indexOf(".")+3);
	                            dLat = dLat.substring(0, dLat.indexOf(".")+3);
	                            dLon = dLon.substring(0, dLon.indexOf(".")+3);
	                            
	                            if(sourceLat.equals(sLat)&&sourceLon.equals(sLon)&&destinationLat.equals(dLat)&&destinationLon.equals(dLon)){
	                                String key = dir.getStartLatLon()+dir.getEndLatLon();
	                                map.put(key, dir);
	                            }
	                        }
	                        return map;
	                    }).collect(Collectors.toList());
	                     
	                    Map<String, DirectionsObject> result = new HashMap<>();
	 
	                    mappedDestination.forEach((p) -> {
	                        p.forEach((k, v) -> result.merge(k, v, (v1, v2) -> {
	                            v2.getJsonList().forEach(v1::addJSON);
	                            return v1;
	                        }));
	                    });
	            		                	
	                    out.writeObject(result);
	                    out.flush();
					}
					else if(message.equals("make a query to Google Directions API")){
						String srcLatLng=(String) in.readObject();
						String dstLatLng=(String) in.readObject();
						
						String urlString = "https://maps.googleapis.com/maps/api/directions/json?sensor=false&origin="+srcLatLng+"&destination="+dstLatLng+"&key=AIzaSyBlres1SjOgsMH-KVBfAvEUwvJpyxQHMzI";
						String data= "";
						
						InputStream istream=null;
						HttpURLConnection urlConnection= null;
						
						try{
				            URL url = new URL(urlString);
				            urlConnection = (HttpURLConnection)url.openConnection();			            
				            urlConnection.connect();			            
				            istream= urlConnection.getInputStream();
				            BufferedReader br = new BufferedReader(new InputStreamReader(istream));			            
				            StringBuffer sb= new StringBuffer();
				            
				            String line= "";
				            while ( (line= br.readLine()) != null ) {
				            	sb.append(line+"\n");
				            	
				            }
				            
				            data= sb.toString();
				            DirectionsObject direction = new DirectionsObject();
				            direction.setEndLatLon(dstLatLng);
				            direction.setStartLatLon(srcLatLng);
				            direction.setJson_string(data);
				            directionsObject_list.add(direction);
				            out.writeObject(direction);
				            out.flush();
			            	data += "\n";
				            
				            Files.write(Paths.get(filename), data.getBytes(), StandardOpenOption.APPEND);
				            br.close();			            
				            istream.close();
				        	urlConnection.disconnect();
				        }catch(Exception e)
				        {
				            System.out.println(e);
				        }
					}				
					in.close();
					out.close();
					connection.close();
				}
			}
		}catch(IOException ioException){
				ioException.printStackTrace();
		}catch(ClassNotFoundException e){
				e.printStackTrace();
		}finally{
			try{
				server_socket.close();
			}catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
}
