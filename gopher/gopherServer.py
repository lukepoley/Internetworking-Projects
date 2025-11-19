'''
A simple TCP "echo" server written in Python.

author:  Amy Csizmar Dalal and Luke Poley, Sam Lengyel, Alden Harcourt
CS 331, Fall 2025

Minimal Gopher server (RFC-style behaviour for menus and text files).
It serves files from the ./content/directory and responds with either a menu (directory listing) or a text file
Menu and text responses are terminated with a single period
To test, use the default port `localhost 48999`.
'''
import sys, socket, os
DEFAULT_PORT = 48999

# Defines the logic for our Gopher server.
class GopherServer:
    # Sets up the server socket
    def __init__(self, port=DEFAULT_PORT):
        self.port = port
        self.host = ""
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((self.host, self.port))

    # Waits for connections and handles them.
    def listen(self):
        self.sock.listen(5)

        while True:
            clientSock, clientAddr = self.sock.accept()
            print ("Connection received from ",  clientSock.getpeername())
            while True:
                data = clientSock.recv(1024)
                if not data:
                    break 


                selector = data.decode("ascii")
                if selector != '\r\n':
                    selector = selector.strip()
                print(f"Received selector: '{selector}'")

                # Determine which resource the client wants based on the selector.
                try:
                    if selector == '' or selector == "\r\n":
                        # An empty selector means the client wants the main menu.
                        resource_path = "content/links.txt"
                        with open(resource_path, "rb") as f:
                            response = b'1' + f.read()
                        clientSock.sendall(response)
                    elif selector.endswith('/'):
                        # A selector ending in '/' is a directory request.
                        path = selector.strip('/')
                        menu_path = os.path.join("content", path, "links.txt")
                        with open(menu_path, "rb") as f:
                            response = b'1' + f.read()
                        clientSock.sendall(response)
                    else:
                        # Otherwise, it's a request for a specific file.
                        resource_path = "content/" + selector
                        with open(resource_path, "rb") as f:
                            # For files, prepend '0' and append the period.
                            response = (f.read()).decode("ascii")
                            response += "\n."
                            response = response.encode("ascii")
                            response = b'0' + response
                        clientSock.sendall(response)

                except FileNotFoundError:
                    # If the file or directory doesn't exist, send a Gopher error message.
                    print(f"Resource not found for selector: '{selector}'")
                    error_msg = f"3'{selector}' not found.\terror\terror\r\n".encode("ascii")
                    clientSock.sendall(error_msg)
                
                # We've sent our response, so break this inner loop.
                break
            clientSock.close()

# Sets up and starts the server based on command-line arguments.
def main():
    if len(sys.argv) > 1:
        try:
            server = GopherServer(int(sys.argv[1]))
        except ValueError as e:
            print ("Error in specifying port. Creating server on default port.")
            server = GopherServer()
    else:
        server = GopherServer()

    # Listen forever
    print ("Listening on port " + str(server.port))
    server.listen()

main()
