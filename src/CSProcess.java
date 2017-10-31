import java.io.*;
import java.net.*;
import java.util.*;

class CSProcess extends Thread {
    protected BufferedReader In;    	// captures incoming messages
    protected PrintWriter 	 Out;   	// sends outgoing messages
    
    protected CommServer server;
    protected Socket socket;
    protected String ip;
    
    protected String UserName;
    protected String roomName = "LOBBY";
    
    protected boolean online = false;
    protected boolean inRoom = false;	// (�濡 �ִ�:true, ���ǿ� �ִ�:false)
    protected int checkcode;

	public CSProcess(CommServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.ip     = socket.getInetAddress().getHostAddress();	
		
        try {
			In  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Out = new PrintWriter(socket.getOutputStream(), true);
			
        } catch(IOException ioe) {
            server.writeActivity("Client IP: " + ip + " could not be " 
            		+ "initialized and has been disconnected.");
            killClient();
        }		
	}

	public void run() {
		try {
			char charBuffer[] = new char[1];		
		 
			while(In.read(charBuffer,0,1) != -1) {
				StringBuffer stringBuffer = new StringBuffer(8192);
				
                // while the stream hasn't ended
                while(charBuffer[0] != '\0') {
                    stringBuffer.append(charBuffer[0]);
                    In.read(charBuffer, 0 ,1);
                }
                
                String strMsg = stringBuffer.toString();
                StringTokenizer st = new StringTokenizer(strMsg, "|");
                
                server.writeActivity("C_Msg : " + strMsg);
                
                if (strMsg == "")
					break;

				if (strMsg != "") {
					checkcode = Integer.parseInt(st.nextToken());
					
					// Login Ȯ
					if (!online) {
						if (!loginCheck(strMsg))
							break;
					}
					
					if (checkcode == 200) {
						lobbyInfo("201");		// Lobby ���� ����.
					}
					if (checkcode == 210) {
						lobbyID();
					}
					if (checkcode == 400) {
						CreateRoom(strMsg);		// Room ����.
					}
					if (checkcode == 500) {
						JoinRoom(strMsg);		// Room ����.
					}
					if (checkcode == 510) {		
						ExitRoom();				// ��ȭ�� ������.
					}
					if (checkcode == 220) {
						RoomInfo(strMsg);		// Room ����.
					}
					if (checkcode == 600) {
						RoomMsg(strMsg);		// ��ȭ���� ����.

						/*
						for (int i = 0; i < server.totalClient.size(); i++) {
							CSProcess temp = (CSProcess) server.totalClient.elementAt(i);
						}	
						*/					
					}
					if (checkcode == 610) {
						TalkMsg(strMsg);		// �ͼӸ� ��ȭ���� ���� (�Ѹ�).
					}
					if (checkcode == 620) {
						FlashMsg(strMsg);		// �÷��� �� ����.(�� ������)
					}
					if (checkcode == 900) {
						QuitServer(strMsg);
					}
				}
			}
		} catch (IOException ioe) {
            server.writeActivity("Client IP: " + ip + " caused a read error " 
                    + ioe + " : " + ioe.getMessage() + "and has been disconnected.");
        } finally {
        	if (online) {
        		server.sendMessageLobby("901|"+ this.UserName + "|" +this.roomName);
        	}
        	
        	if (inRoom) {
        		server.sendMessageGroup("901|" + this.UserName + "|" + this.roomName, this.roomName);
        		server.removeRoomConnection(this, roomName);        		
        	}
        	
        	killClient();
            // ���ο� Lobby ���� ����.
            // lobbyInfo("201");
        }
	}

	public boolean loginCheck(String message) {
		int index;
		String idCode = message.substring(0, index=message.indexOf('|'));
		UserName = message.substring(++index);

		if (server.hasName(UserName)) {
			// ���� ID�� ���� �� ��� 
			broadcastMessage("102|" + UserName);

			return false;
		}

		server.addUserName(UserName);
		online = true;
		
		server.sendMessageLobby("101|" + UserName);
		return true;
	}
	
	public void lobbyInfo(String PID) {
		// 201|�������ڼ�|����ڼ�|������ ���|���̸�^������^�ο�����|��
		// 201|�������ڼ�|����ڼ�|������ ���|���̸�^������|��

		String message  = PID;
		message += "|";
		message += server.TOTOL_USER;
		message += "|";
		message += server.clients.size();
		message += "|";
		message += server.roomName.size();

		if (server.roomName.size() > 0) {
			for (int i = 0; i < server.roomName.size(); i++) {
				message += "|";
				message += server.roomName.elementAt(i);

				Vector GUser = (Vector) server.csRoom.get(server.roomName.elementAt(i));
				message += "^";
				message += GUser.size();
			}			
		}

		broadcastMessage(message);
	}
	
	public void lobbyID() {
		// 211|�������ڼ�
		String message = "211";
		message += "|" + UserName + "|";
		message += server.TOTOL_USER;
		//message += "|";
		//message += server.clients.size();
		// message += "|";
		/*
		for (int i = 0; i < server.clients.size(); i++) {
			CSProcess temp = (CSProcess) server.clients.elementAt(i);

			message += "|";			
			message += temp.UserName;
		}
		*/
		//broadcastMessage(message);		
		server.sendMessageLobby(message);
	}
	
	public void CreateRoom(String message) {
		// 400|���̸�|�ο�����|��й�ȣ
		int index;
		String idCode = message.substring(0, index=message.indexOf('|'));
		//roomName      = message.substring(++index, index=message.indexOf('|', index));
		//String limitUser  = message.substring(++index, index=message.indexOf('|', index));
		//String roomPasswd = message.substring(++index);
		roomName      = message.substring(++index);
			
		if(server.makeRoom(this, roomName)){
			broadcastMessage("401|" + roomName);
			server.sendMessageLobby("402|" + UserName + "|" + roomName);
			
			inRoom = true;
		} else {
			broadcastMessage("403|" + roomName);
		}
	}
	
	public void JoinRoom(String message) {
		inRoom = true;
		
		int index;
		String idCode = message.substring(0, index=message.indexOf('|'));
		roomName      = message.substring(++index);
		
		
		if (server.hasRoomName(roomName)) {
			server.addUsertoRoom(this, roomName);
	
			server.sendMessageGroup("501|" + UserName + "|" + roomName, roomName);
			server.sendMessageLobby("501|" + UserName + "|" + roomName);
		} else {
			broadcastMessage("502|" + roomName);
		}
	}
	
	public void RoomInfo(String message) {
		// 221|�����ڼ�|ID|ID|ID|��

		int index;
		String idCode = message.substring(0, index=message.indexOf('|'));
		roomName      = message.substring(++index);
		
		Vector GUser = (Vector) server.csRoom.get(roomName);

		int count = 0;
		String userID = "";
		
		for (int i = 0; i < GUser.size(); i++) {
			CSProcess temp = (CSProcess) GUser.elementAt(i);
			userID += "|" + temp.UserName;
			count++;
		}
		
		broadcastMessage("221|" + count + userID);
	}
	
	public void RoomMsg(String msg) {
		int index;
		String idCode = msg.substring(0, index=msg.indexOf('|'));
		String m = msg.substring(++index);
		
		// �ڵ�|�����|�޼���.
/*
		String message  = "601";
		message += "|";
		message += UserName;
		message += "|";
		message += m;
*/
		//server.sendMessageGroup(message, roomName);
		server.sendMessageLobby("601|" + UserName + "|" + m);

	}
	
	public void TalkMsg(String msg) {
		int index;
		String idCode = msg.substring(0, index=msg.indexOf('|'));
		String sendName = msg.substring(++index, index=msg.indexOf('|', index));
		String m = msg.substring(++index);
		
		// �ڵ�|�����|�޼���.
		String message  = "611";
		message += "|";
		message += UserName;
		message += "|";
		message += sendName;
		message += "|";
		message += m;

		server.sendMessageTalk(sendName, message);
	}
	
	public void FlashMsg(String msg) {
		int index;
		String idCode  = msg.substring(0, index=msg.indexOf('|'));
		String flashID = msg.substring(++index);
		
		// �ڵ�|�����|�޼���.
		String message  = "621";
		message += "|";
		message += UserName;
		message += "|";
		message += flashID;

		server.sendMessageGroup(message, roomName);		
	}
	
	public void ExitRoom() {
		// 511|ID|���̸� (������, �����)
		server.sendMessageGroup("511|"+ UserName + "|" + roomName, roomName);
		server.sendMessageLobby("511|" + UserName + "|" + roomName);
		
		server.outRoom(this, roomName);
		
		inRoom = false;
		checkcode = 1;
	}
	
	public void QuitServer(String message) {
		online = false;
		System.out.println("CCCC");
		server.sendMessageLobby("901|" + UserName + "|" + roomName);
		
		killClient();
	}
	
    /**
     * Sends a message to this client. Called by the server's broadcast method.
     * @param   message    The message to send.
    */
    public void broadcastMessage(String message) {
    	message += '\0';
    	
    	Out.print(message);
    	
    	server.writeActivity("S_Msg : " + message);
        
        // --- flush the buffer and check for errors
        // --- if error then kill this client
        if(Out.checkError()) {
            server.writeActivity("Client IP: " + ip + " caused a write error "
            + "and has been disconnected.");
            // killClient();
        }
    }

    public String getIP() {
        return ip;
    }
    
    private void killClient() {
        // tell the server to remove the client from the client list  
    	server.removeClient(this);
        if (online) {
        	server.removeUserName(UserName);
        }
        
        server.TOTOL_USER--;
        if (server.TOTOL_USER < 0) {
        	server.TOTOL_USER = 0;
        }

        try {
            In.close();
            Out.close();
            socket.close();            
        } catch (IOException ioe) {
            server.writeActivity("Client IP: " + ip + " caused an error "
            		+ "while disconnecting.");
        }
    }
}
