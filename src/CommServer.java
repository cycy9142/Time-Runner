import java.io.*;
import java.net.*;
import java.util.*;

/**
 * <p>MF Generic Flash Communication Server.</p>
 * 
 * Usage: java CommServer [port]
 *
 * @version 0.1
 */

public class CommServer { 		// �� Ŭ���̾�Ʈ�� ���� ������ Vector �� ���� */
	ServerSocket server; 		// ���� ���� ���� .....
	Vector UserName;
	Vector clients; 			// ����� ����
	Vector totalClient;			// ��ü ������ ����.
	Hashtable csRoom; 			// ������ ��ȭ��
	Vector roomName;
	Hashtable roomUserName;
	static int TOTOL_USER = 0;	//������ �� ���� ��.

	public CommServer(int port) {
		clients  = new Vector();
		totalClient = new Vector();
		csRoom   = new Hashtable();
		roomUserName = new Hashtable();
		roomName = new Vector();
		UserName = new Vector();	
		
		writeActivity("MFCommServer to Start Server");

		try {
			server = new ServerSocket(port);
			writeActivity("Server Started on Port: " + port);
			while (true) {
				Socket socket = server.accept();
				
				// Ŭ���̾�Ʈ���� ��ü ����
				CSProcess client = new CSProcess(this, socket);
				TOTOL_USER++;
				
				writeActivity(client.getIP() + " connected to the server.");
				clients.addElement(client);
				client.start();
				
				totalClient.addElement(client);
			}
		} catch (Exception e) {
			writeActivity("Server Error...Stopping Server");
			killServer();
		}	
	}

	synchronized public boolean addUserName(String name) {
		boolean test = UserName.add(name);

		return test;
	}
	
	public void removeUserName(String name) {
		UserName.remove(name);
	}

	synchronized public void addUsertoRoom(CSProcess c, String Bangname) {
		Vector vtemp = new Vector();
		vtemp = (Vector) csRoom.get(Bangname);
		vtemp.add(c);
		
		clients.remove(c);
		csRoom.put(Bangname, vtemp);
		
		Vector BANGUserList = new Vector();
		BANGUserList = (Vector) roomUserName.get(Bangname);
		roomUserName.remove(Bangname);
		BANGUserList.add(((String) c.UserName).trim());
		roomUserName.put(Bangname, BANGUserList);
	}
	
	synchronized public boolean hasRoomName(String str) {
		boolean test = false;
		for (int i = 0; i < roomName.size(); i++) {
			if (str.equals((String) roomName.elementAt(i))) {
				test = true;
				break;
			}
		}

		return test;
	}

	// ���� �̸��� �ִ��� Ȯ��..���߿� Login���� ��ü ����...
	synchronized public boolean hasName(String str) {
		boolean checkName = false;
		for (int i = 0; i < UserName.size(); i++) {
			if (str.equals((String) UserName.elementAt(i))) {
				checkName = true;
				break;
			}
		}

		return checkName;
	}
	
	public boolean makeRoom(CSProcess c, String Bangname) {
		if(!hasRoomName(Bangname)){
			Vector vtemp = new Vector();
			vtemp.add(c);
			clients.remove(c);
			csRoom.put(Bangname, vtemp);
			roomName.add(Bangname);
			
			Vector BANGUserList = new Vector();
			BANGUserList.add(((String) c.UserName).trim());
			roomUserName.put(Bangname, BANGUserList);
			return true;
		} else {			
			return false;
		}
	}

	public void outRoom(CSProcess c, String Bangname) {
		Vector vtemp = new Vector();
		vtemp = (Vector) csRoom.get(Bangname);
		Vector BANGUserList = new Vector();
		BANGUserList = (Vector) roomUserName.get(Bangname);

		if (vtemp.size() > 1) {
			vtemp.remove(c);
			csRoom.remove(Bangname);
			csRoom.put(Bangname, vtemp);
			clients.add(c);

			BANGUserList.remove(((String) c.UserName).trim());
			roomUserName.remove(Bangname);
			roomUserName.put(Bangname, BANGUserList);
		} else {
			vtemp.remove(c);
			clients.add(c);
			csRoom.remove(Bangname);
			roomName.remove(Bangname);
			roomUserName.remove(Bangname);
		}
		
		for (int i = 0; i < totalClient.size(); i++) {
			CSProcess temp = (CSProcess) totalClient.elementAt(i);
			
			if (c == temp) {
				temp.roomName = "LOBBY";
			}
		}
	}
		
	// ���� ���ǿ� �ִ� ����鿡�Ը� Message�� ������.
	public void sendMessageLobby(String message) {
		for (int i = 0; i < clients.size(); i++) {
			CSProcess client = (CSProcess) clients.elementAt(i);
			// if (!client.inRoom) client.broadcastMessage(message);
			client.broadcastMessage(message);
		}
	}	

	// ���� �濡 �ִ� ����忡�Ը� Message�� ����.
	public void sendMessageGroup(String str, String Bangname) {
		Vector GUser = (Vector) csRoom.get(Bangname);

		for (int i = 0; i < GUser.size(); i++) {
			CSProcess temp = (CSProcess) GUser.elementAt(i);
			temp.broadcastMessage(str);
		}
	}	

	public void sendMessageTalk(String sendID, String str) {
		for (int i = 0; i < totalClient.size(); i++) {
			CSProcess temp = (CSProcess) totalClient.elementAt(i);
			
			if (sendID.equals(temp.UserName)) {	
				temp.broadcastMessage(str);
			}
		}
	}
	
	public void sendMessageTalk_old(String sendID, String str, String Bangname) {
		// �濡 �ִ� ������Ը� �ӼӸ� ������.
		Vector GUser = (Vector) csRoom.get(Bangname);

		for (int i = 0; i < GUser.size(); i++) {
			CSProcess temp = (CSProcess) GUser.elementAt(i);
			
			if (sendID.equals(temp.UserName)) {	
				temp.broadcastMessage(str);
			}
		}
	}
	
	public void removeRoomConnection(CSProcess c, String Bangname) {
		Vector vtemp = new Vector();
		vtemp = (Vector) csRoom.get(Bangname);
		Vector BANGUserList = new Vector();
		BANGUserList = (Vector) roomUserName.get(Bangname);

		if (vtemp.size() > 1) {
			vtemp.remove(c);
			csRoom.remove(Bangname);
			BANGUserList.remove(((String) c.UserName).trim());
			roomUserName.remove(Bangname);
			roomUserName.put(Bangname, BANGUserList);

			csRoom.put(Bangname, vtemp);
		} else {
			vtemp.remove(c);
			csRoom.remove(Bangname);
			roomName.remove(Bangname);
			roomUserName.remove(Bangname);
		}
	}
	
    /**
     * Removes clients from the client list.
     * @param   client    The CSClient to remove.
    */
    public void removeClient(CSProcess client) {
        writeActivity(client.getIP() + " has left the server.");
        clients.removeElement(client);
        totalClient.removeElement(client);
    }	
	
	// stop the server
    private void killServer() {
        try {            
            server.close();
            writeActivity("Server Stopped");
        } catch (IOException ioe) {
            writeActivity("Error while stopping Server");
        }
    }
	
    /**
     * Writes a message to System.out.println in the format
     * [mm/dd/yy hh:mm:ss] message.
     * @param   activity    The message.
    */
    public void writeActivity(String activity) {
        Calendar cal = Calendar.getInstance();
        activity = "[" + cal.get(Calendar.YEAR) 
                 + "/" + cal.get(Calendar.MONTH) 
                 + "/" + cal.get(Calendar.DAY_OF_MONTH) 
                 + " " 
                 + cal.get(Calendar.HOUR_OF_DAY) 
                 + ":" + cal.get(Calendar.MINUTE) 
                 + ":" + cal.get(Calendar.SECOND) 
                 + "] " + activity + "\n";

        System.out.print(activity);
    }	
	
    public static void main(String args[]) {
        if(args.length == 1) {
            new CommServer(Integer.parseInt(args[0]));
        } else {
            System.out.println("Usage: java CommServer [port]");
        }
    }
}
