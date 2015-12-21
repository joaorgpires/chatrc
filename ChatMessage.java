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

enum MessageType {
    OK, ERROR, MESSAGE, NEWNICK, JOINED, LEFT, BYE, PRIVATE
}

public class ChatMessage {
    private MessageType type;
    private String message1;
    private String message2;

    static private String okRegex = "OK";
    static private String errorRegex = "ERROR";
    static private String messageRegex = "MESSAGE .+ .+";
    static private String newnickRegex = "NEWNICK .+ .+";
    static private String joinedRegex = "JOINED .+";
    static private String leftRegex = "LEFT .+";
    static private String byeRegex = "BYE";
    static private String privateRegex = "PRIVATE .+ .+";
    
    public ChatMessage(MessageType type) {
	this.type = type;
	message1 = "";
	message2 = "";
    }
    
    public ChatMessage(MessageType type, String message1) {
	this.type = type;
	this.message1 = message1;
	message2 = "";
    }
    
    public ChatMessage(MessageType type, String message1, String message2) {
	this.type = type;
	this.message1 = message1;
	this.message2 = message2;
    }
    
    public MessageType getType() {
	return this.type;
    }
    
    public String toString() {
	return this.toString(false);
    }
    
    public String toString(boolean amicable) {
	String output = "";
    
	switch (this.type) {
	case OK:
	    if(amicable) {
		output = "Command successful";
	    }
	    else {
		output = "OK";
	    }
	    break;
	case ERROR:
	    if(amicable) {
		output = "Error found: " + this.message1;
	    }
	    else {
		output = "ERROR " + this.message1;
	    }
	    break;
	case MESSAGE:
	    if(amicable) {
		output = this.message1 + " says: " + this.message2;
	    }
	    else {
		output = "MESSAGE " + this.message1 + " " + this.message2;
	    }
	    break;
	case NEWNICK:
	    if(amicable) {
		output = this.message1 + " changed his nick to " + this.message2;
	    }
	    else {
		output = "NEWNICK " + this.message1 + " " + this.message2;
	    }
	    break;
	case JOINED:
	    if(amicable) {
		output = this.message1 + " has joined the room";
	    }
	    else {
		output = "JOINED " + this.message1;
	    }
	    break;
	case LEFT:
	    if(amicable) {
		output = this.message1 + " left the room";
	    }
	    else {
		output = "LEFT " + this.message1;
	    }
	    break;
	case BYE:
	    if(amicable) {
		output = "Disconnected... Press enter to exit";
	    }
	    else {
		output = "BYE";
	    }
	    break;
	case PRIVATE:
	    if(amicable) {
		output = "<private message from " + this.message1 + ">: " + this.message2;
	    }
	    else {
		output = "PRIVATE " + this.message1 + " " + this.message2;
	    }
	    break;
	}
    
	return output + "\n";
    }

    
    public static ChatMessage parseString(String s) {
	MessageType type;
	String message1 = "";
	String message2 = "";
	
	String[] parts = s.split(" ");
	
	if(Pattern.matches(okRegex, s)) {
	    type = MessageType.OK;
	}
	else if(parts[0].equals("ERROR")) {
	    //System.out.println("deu asneira aqui");
	    type = MessageType.ERROR;
	    String finalMessage = "";
	    
	    for (int i = 1; i < parts.length; i ++) {
		if(i > 1)
		    finalMessage += " ";
		finalMessage += parts[i];
	    }
	    
	    message1 = finalMessage;
	}
	else if(Pattern.matches(messageRegex, s)) {
	    type = MessageType.MESSAGE;
	    message1 = s.split(" ")[1];
	    int position = s.substring(7).indexOf(message1);
	    message2 = s.substring(7 + position + message1.length());
	}
	else if(Pattern.matches(newnickRegex, s)) {
	    type = MessageType.NEWNICK;
	    message1 = s.split(" ")[1];
	    message2 = s.split(" ")[2];
	}
	else if(Pattern.matches(joinedRegex, s)) {
	    type = MessageType.JOINED;
	    message1 = s.split(" ")[1];
	}
	else if(Pattern.matches(leftRegex, s)) {
	    type = MessageType.LEFT;
	    message1 = s.split(" ")[1];
	}
	else if(Pattern.matches(byeRegex, s)) {
	    type = MessageType.BYE;
	}
	else if(Pattern.matches(privateRegex, s)) {
	    type = MessageType.PRIVATE;
	    message1 = s.split(" ")[1];
	    int position = s.substring(7).indexOf(message1);
	    message2 = s.substring(7 + position + message1.length());
	}
	else {
	    //System.out.println("Foi aqui");
	    type = MessageType.ERROR;
	    String finalMessage = "";
	    
	    for (int i = 1; i < parts.length; i ++) {
		if(i > 1)
		    finalMessage += " ";
		finalMessage += parts[i];
	    }
	    
	    message1 = finalMessage;
	}
	
	return new ChatMessage(type, message1, message2);
    }
}
