_Sample collab.txt file -- remove this text and replace with your own_

# Collaborations

## People

* Joanna Smith helped me find a bug which caused my SimpleTCPServer to hang.
* Pat Jones helped me figure out how to send multiple messages to the SimpleTCPServer at once.


## Websites / online resources

* StackOverflow: ["how do I send a file over a socket in python"](https://stackoverflow.com/questions/27241804/sending-a-file-over-tcp-sockets-in-python) (Google search)


## AI tools used

### Gemini

Prompt: what does this block of Python code do:
```
while True:
    data = clientSock.recv(1024)
    if not len(data):
        break
    print ("Received message:  " + data.decode("ascii"))

    clientSock.sendall(data)`
```

answer:

This Python code block receives and processes data from a network socket. It continuously listens for incoming messages, prints them to the console, and then sends the exact same data back to the sender. This process repeats indefinitely until the connection is closed by the client.

How the Code Works ...

