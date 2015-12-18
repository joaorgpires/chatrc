import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.charset.*;

enum State {INIT, OUTSIDE, INSIDE}
public class ChatUser {
    private String nick;
    private State userState;
    private SocketChannel sc;
    private ChatRoom room;
    
    public ChatUser(SocketChannel sc) {
	this.nick = "";
	this.userState = State.INIT;
	this.sc = sc;
	this.room = null;
    }
    
    /*public int cmpTo(ChatUser b) {
	return this.nick.compareTo(b.getNick());
	}*/
    
    public void joinRoom(ChatRoom room) {
	this.room = room;
    }
    
    public ChatRoom getRoom() {
	return this.room;
    }
    
    public void leaveRoom() {
	this.room = null;
    }
    
    public void setNick(String nick) {
	this.nick = nick;
    }
    
    public String getNick() {
	return this.nick;
    }

    public SocketChannel getSocketChannel() {
	return this.sc;
    }
}
