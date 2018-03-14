import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Reduce_Worker {
	private static ArrayList<DirectionsObject> dataFromMapper = new ArrayList<DirectionsObject>();
	private static ArrayList<DirectionsObject> dataForMaster = new ArrayList<DirectionsObject>();

	public static void main(String[] args){
		reducerServer();
	}
	
	public static void reducerServer(){
		ServerSocket providerSocket = null;
		Socket connection = null;
		String message = null;
		
		try{
			providerSocket = new ServerSocket(4320);
			while(true){
				System.out.println("waiting for connnection");
				connection = providerSocket.accept();
				
				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
				
				out.writeObject("Connection succesfull !");
				out.flush();
				
				ArrayList<Map<String,DirectionsObject>> dataFromMapper = null;
				message = (String) in.readObject();
				if(message.equalsIgnoreCase("wait for List<Map<String,DirectionsObject>> list")){
					out.writeObject("waiting for List<Map<String,DirectionsObject>> list");
					out.flush();
					dataFromMapper = (ArrayList<Map<String,DirectionsObject>>) in.readObject();
				}
				
				out.writeObject(reduce(dataFromMapper));
				
				out.flush();
				
				in.close();
				out.close();
				connection.close();
			}
		}catch(IOException ioException){
				ioException.printStackTrace();
		}catch(ClassNotFoundException e){
				e.printStackTrace();
		}finally{
			try{
				providerSocket.close();
			}catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	
	public static Map<String,DirectionsObject> reduce(ArrayList<Map<String,DirectionsObject>> dataFromMapper){
		Map<String, DirectionsObject> result = dataFromMapper.stream().reduce((p1, p2) -> {
			p2.forEach((k, v) -> p1.merge(k, v, (v1, v2) -> {
				v2.getJsonList().forEach(v1::addJSON);
				v1.setJsonList((ArrayList<String>) v1.getJsonList().stream().distinct().collect(Collectors.toList()));
				return v1;
			}));
			return p1;
		}).get();

		return result;
	}
	
}
