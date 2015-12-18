import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class ChatRoom {
    private String name;
    private Map<String, ChatUser> users;
    
    public ChatRoom(String name) {
	this.name = name;
	//Guaranteed log(n) cost for containsKey, get, put and remove ops
	this.users = new HashMap<String, ChatUser>();
    }
    
    public String getName() {
	return this.name;
    }
    
    public ChatUser[] getUsers() {
	return this.users.values().toArray(new ChatUser[this.users.size()]);
    }
    
    public void addUser(ChatUser user) {
	this.users.put(user.getNick(), user);
    }
    
    public void removeUser(ChatUser user) {
	this.users.remove(user.getNick());
    }
}
