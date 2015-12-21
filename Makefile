JAVA = javac

RM = /bin/rm -f

chat: 
	${JAVA} ChatClient.java
	${JAVA} ChatUser.java
	${JAVA} ChatRoom.java
	${JAVA} ChatServer.java
	${JAVA} ChatMessage.java

clean:
	${RM} *.class
