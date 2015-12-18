import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer {
    //Users, Nicks and Rooms
    private Map<SocketChannel, ChatUser> users = new HashMap<SocketChannel, ChatUser>();
    private Map<String, ChatUser> nicks = new HashMap<String, ChatUser>();
    private Map<String, ChatRoom> rooms = new HashMap<String, ChatRoom>();
    //Ports from 0 to 65535
    
    
}
