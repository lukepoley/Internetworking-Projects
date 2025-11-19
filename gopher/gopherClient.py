'''
A simple gopher client written in Python.

author:  Starter code by Amy Csizmar Dalal; Expansion by Alden Harcourt, Sam Lengyel, Luke Poley
CS 331, Fall 2025

Minimal Gopher client (RFC-style behaviour for menus and text files).
It sends messages to and processes replies from a Gopher server following RFC 1436.
To test, use the default port of the server you're using.
'''
import sys, socket

def usage():
    print ("Usage:  python SimpleTCPClient <server IP> <port number> <message>")
    sys.exit()

def main():
    # Process command line args (server, port, message)
    if len(sys.argv) < 3:
        usage()

    # Allow the user to enter a blank message (simluationg just hitting enter) without ignoring the message if the user DOES send it.
    try:
        server = sys.argv[1]
        port = int(sys.argv[2])
        if len(sys.argv) > 3:
            message = sys.argv[3]
        else:
            message = ''
    except ValueError as e:
        usage()

    # Actually connect to the server.
    serverSock = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
    serverSock.connect((server, port))
    print ("Connected to server; sending message")

    # Allow the user to send an empty message (same as sending a carriage return) to request the contents of the server's links.txt file.
    if message == '':
        message = '\r\n'

    # Actually send the message; Both server and client encode and decode in ascii.
    serverSock.send(message.encode("ascii"))
    print ("Sent message; waiting for reply")

    # Receive at maximum 64 kilobytes at once.
    returned = serverSock.recv(65536)

    # Defined Item-Type Character in Gopher for Files, Directories, Error Messages
    FILE = '0'
    DIRECTORY = '1'
    ERROR = '3'
    YES = "Y".casefold()
    NO = "N".casefold()


    reply = returned.decode("ascii")
    replyType = reply[0] # pull the file type from the first character received
    replyCleaned = reply[1:] # separate Type from Reply

    # Special case - if the Server sends an error back to the client handle that different from a normal message.
    if replyType == ERROR:
        # Special case if the user tries to use *Nix parent directory operator. May expand later to allow going up a level with this.
        if reply[1] == '.' and reply [2] == '.':
            print ("ERROR - \"..\" not a valid selector string")
        # Otherwise tell the user that they just asked for an invalid file or directory.
        else:
            print ("ERROR - " + message + " not a valid file or directory")

    # Directories are simple, just send the message received because that's the links.txt of the directory
    elif replyType == DIRECTORY:
        print ("Received reply: \n" + replyCleaned)

    # Files are similar to directories, but we also want an option for the user to download the file.
    elif replyType == FILE:
        # First print its contents
        print("Received reply: \n" + replyCleaned)

        # Then check if the user wants to download it.
        downloadYN = input("Download " + message  +" Y/N: ").casefold()
        if downloadYN == YES:
            savePath = input("Enter the path to where you want to save " + message + " or press enter to save the file in your current directory: ") + message
            with open(savePath, "wb") as downloadedFile:
                downloadedFile.write(replyCleaned.encode("ascii"))
            print(message + " downloaded to " + savePath)

    serverSock.close() # clean up after ourselves.

main()
