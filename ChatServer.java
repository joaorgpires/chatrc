import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;

public class ChatServer {
    static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );
    
    //Users, Nicks and Rooms
    static private Map<SocketChannel, ChatUser> users = new HashMap<SocketChannel, ChatUser>();
    static private Map<String, ChatUser> nicks = new HashMap<String, ChatUser>();
    static private Map<String, ChatRoom> rooms = new HashMap<String, ChatRoom>();
    //Ports from 0 to 65535
    
    static private String nickRegex = "nick .+";
    static private String joinRegex = "join .+";
    static private String leaveRegex = "leave.*";
    static private String byeRegex = "bye.*";
    static private String privateRegex = "priv .+ .+";
    
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();
    static private final CharsetEncoder encoder = charset.newEncoder();
    
    static public void main(String args[]) throws Exception {
	//Just code from pl6
	int port = Integer.parseInt(args[0]);
	
	try {
	    ServerSocketChannel ssc = ServerSocketChannel.open();
	    ssc.configureBlocking(false);
	    
	    ServerSocket ss = ssc.socket();
	    InetSocketAddress isa = new InetSocketAddress(port);
	    ss.bind(isa);
	    
	    Selector selector = Selector.open();

	    ssc.register(selector, SelectionKey.OP_ACCEPT);
	    System.out.println("Chat listening on port " + port);

	    while (true) {
		int num = selector.select();

		if (num == 0) {
		    continue;
		}

		Set<SelectionKey> keys = selector.selectedKeys();
		Iterator<SelectionKey> it = keys.iterator();
        
		while (it.hasNext()) {
		    SelectionKey key = it.next();

		    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
			Socket s = ss.accept();
			System.out.println("Got connection from " + s);

			SocketChannel sc = s.getChannel();
			sc.configureBlocking(false);

			sc.register(selector, SelectionKey.OP_READ);
			users.put(sc, new ChatUser(sc));
		    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
			SocketChannel sc = null;

			try {
			    sc = (SocketChannel) key.channel();
			    boolean ok = processInput(sc);

			    if (!ok) {
				key.cancel();
				closeClient(sc);
			    }

			} catch (IOException ie) {
			    key.cancel();
			    closeClient(sc);
			}
		    }
		}

		keys.clear();
	    }
	} catch (IOException ie) {
	    System.err.println(ie);
	}
    }
    
    static private void closeClient(SocketChannel sc) {
	Socket s = sc.socket();
	try {
	    System.out.println("Closing connection to " + s);
	    sc.close();
	} catch (IOException ie) {
	    System.err.println("Error closing socket " + s + ": " + ie);
	}

	if (!users.containsKey(sc))
	    return;

	ChatUser sender = users.get(sc);
	if (sender.getState() == State.INSIDE) {
	    ChatRoom room = sender.getRoom();
	    room.removeUser(sender);
	    ChatUser[] userList = room.getUsers();

	    for (ChatUser user : userList) {
		try {
		    sendLeftMessage(user, sender.getNick());
		} catch (IOException ie) {
		    System.err.println("Error sending left message: " + ie);
		}
	    }

	    if (userList.length == 0)
		rooms.remove(room.getName());
	}

        nicks.remove(sender.getNick());
	users.remove(sc);
    }
    
    static private boolean processInput(SocketChannel sc) throws IOException {
	buffer.clear();
	sc.read(buffer);
	buffer.flip();
	
	if (buffer.limit() == 0)
	    return false;
	
	String message = decoder.decode(buffer).toString().trim();
	ChatUser sender = users.get(sc);
	
	if (message.startsWith("/")) {
	    String escapedMessage = message.substring(1);
	    String cmd = escapedMessage.trim();
	    
	    //Easier with regexs just to make sure everything runs smooth, like for instance nicknames can't have spaces
	    if(Pattern.matches(nickRegex, cmd))
		sendNickCommand(sender, cmd.split(" ")[1]);
	    else if(Pattern.matches(joinRegex, cmd))
		sendJoinCommand(sender, cmd.split(" ")[1]);
	    else if(Pattern.matches(leaveRegex, cmd))
		sendLeaveCommand(sender);
	    else if(Pattern.matches(byeRegex, cmd))
		sendByeCommand(sender);
	    else if(Pattern.matches(privateRegex, cmd))
		sendPrivateCommand(sender, cmd.split(" ")[1], cmd.split(" ")[2]);
	    else if(cmd.startsWith("/"))
		sendSimpleMessage(sender, escapedMessage);
	    else
		sendErrorMessage(sender, "Unknown command");
	}
	else
	    sendSimpleMessage(sender, message);
	
	return true;
    }
    
    static private void sendMessage(SocketChannel sc, ChatMessage message) throws IOException {
	sc.write(encoder.encode(CharBuffer.wrap(message.toString())));
    }
    
    static private void sendLeftMessage(ChatUser user, String nick) throws IOException {
	ChatMessage message = new ChatMessage(MessageType.LEFT, nick);
	sendMessage(user.getSocketChannel(), message);
    }
    
    static private void sendNickCommand(ChatUser sender, String nick) throws IOException {
	if(nicks.containsKey(nick))
	    sendErrorMessage(sender, "Nick " + nick + " already in use");
	else {
	    if(sender.getState() == State.INIT)
		sender.setState(State.OUTSIDE);
	    
	    if(sender.getState() == State.INSIDE) {
		ChatRoom room = sender.getRoom();
		ChatUser[] userList = room.getUsers();

		for (ChatUser user : userList) {
		    if (user != sender) {
			sendNewNickMessage(user, sender.getNick(), nick);
		    }
		}
	    }

	    nicks.remove(sender.getNick());
	    nicks.put(nick, sender);
	    sendOkMessage(sender);
	    sender.setNick(nick);
	}
    }
    
    static private void sendErrorMessage(ChatUser receiver, String message) throws IOException {
	ChatMessage chatMessage = new ChatMessage(MessageType.ERROR, message);
	sendMessage(receiver.getSocketChannel(), chatMessage);
    }
    
    static private void sendNewNickMessage(ChatUser receiver, String oldNick, String newNick) throws IOException {
	ChatMessage message = new ChatMessage(MessageType.NEWNICK, oldNick, newNick);
	sendMessage(receiver.getSocketChannel(), message);
    }
    
    static private void sendOkMessage(ChatUser receiver) throws IOException {
	ChatMessage message = new ChatMessage(MessageType.OK);
	sendMessage(receiver.getSocketChannel(), message);
    }
    
    static private void sendJoinCommand(ChatUser sender, String room) throws IOException {
	if(sender.getState() == State.INIT)
	    sendErrorMessage(sender, "Please set your nickname");
	else {
	    if (!rooms.containsKey(room))
		rooms.put(room, new ChatRoom(room));
      
	    ChatRoom newRoom = rooms.get(room);
	    ChatUser[] userList = newRoom.getUsers();
	    newRoom.addUser(sender);

	    for(ChatUser user : userList)
		sendJoinedMessage(user, sender.getNick());

	    if(sender.getState() == State.INSIDE) {
		ChatRoom oldRoom = sender.getRoom();
		oldRoom.removeUser(sender);
		userList = oldRoom.getUsers();

		for(ChatUser user : userList)
		    sendLeftMessage(user, sender.getNick());
	    }

	    sendOkMessage(sender);
	    sender.joinRoom(newRoom);
	    sender.setState(State.INSIDE);
	}
    }
    
    static private void sendJoinedMessage(ChatUser receiver, String nick) throws IOException {
	ChatMessage message = new ChatMessage(MessageType.JOINED, nick);
	sendMessage(receiver.getSocketChannel(), message);
    }
    
    static private void sendLeaveCommand(ChatUser sender) throws IOException {
	if(sender.getState() != State.INSIDE)
	    sendErrorMessage(sender, "You have to connecet to a room before sending any messages");
	else {
	    ChatRoom room = sender.getRoom();
	    room.removeUser(sender);
	    ChatUser[] userList = room.getUsers();
	    
	    for(ChatUser user : userList)
		sendLeftMessage(user, sender.getNick());
	    
	    if(userList.length == 0)
		rooms.remove(room.getName());
	    
	    sendOkMessage(sender);
	    sender.setState(State.OUTSIDE);
	}
    }
    
    static private void sendByeCommand(ChatUser sender) throws IOException {
	sendByeMessage(sender);
	closeClient(sender.getSocketChannel());
    }
    
    static private void sendByeMessage(ChatUser receiver) throws IOException {
	ChatMessage message = new ChatMessage(MessageType.BYE);
	sendMessage(receiver.getSocketChannel(), message);
    }
    
    static private void sendPrivateCommand(ChatUser sender, String receiver, String message) throws IOException {
	if(sender.getState() == State.INIT)
	    sendErrorMessage(sender, "You have to connecet to a room before sending any messages");
	else {
	    if(nicks.containsKey(receiver)) {
		sendOkMessage(sender);
		sendPrivateMessage(nicks.get(receiver), sender.getNick(), message);
	    }
	    else
		sendErrorMessage(sender, receiver + " is not online");
	}
    }
    
    static private void sendPrivateMessage(ChatUser receiver, String sender, String message) throws IOException {
	ChatMessage chatMessage = new ChatMessage(MessageType.PRIVATE, sender, message);
	sendMessage(receiver.getSocketChannel(), chatMessage);
    }
    
    static private void sendSimpleMessage(ChatUser sender, String message) throws IOException {
	if(sender.getState() == State.INSIDE) {
	    ChatRoom senderRoom = sender.getRoom();
	    ChatUser[] userList = senderRoom.getUsers();
	    
	    for (ChatUser user : userList)
		sendMessageMessage(user, sender.getNick(), message);
	}
	else
	    sendErrorMessage(sender, "You have to connecet to a room before sending any messages");
    }
    
    static private void sendMessageMessage(ChatUser receiver, String sender, String message) throws IOException {
	ChatMessage chatMessage = new ChatMessage(MessageType.MESSAGE, sender, message);
	sendMessage(receiver.getSocketChannel(), chatMessage);
    }
}
