JAVA = javac

RM = /bin/rm -f

chat: 
	${JAVA} ChatClient.java
	${JAVA} ChatUser.java

clean:
	${RM} *.class
