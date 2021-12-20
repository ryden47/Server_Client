#include <stdlib.h>
#include <connectionHandler.h>
#include <iostream>
#include <thread>

using std::cout;
using std::endl;
using std::thread;

ConnectionHandler* connectionHandler = nullptr;
bool terminate = false;
bool logoutCommand = false;

void writeToServer(){
    while(!terminate) {
        if(!logoutCommand) { //wait for a response from server to know if logout was successful or not
            const short bufsize = 1024;
            char buf[bufsize];
            std::cin.getline(buf, bufsize);
            std::string line(buf);
            if (!connectionHandler->sendLine(line)) { //Sending the line to the server.
                std::cout << "Disconnected. Exiting1...\n" << std::endl;
                break;
            }
            if (line == "LOGOUT"){
                logoutCommand = true;
            }
        }
    }
}

void readFromServer(){
    while(1){
        std::string answer; //Getting an answer from the server.
        connectionHandler->getLine(answer);
        std::cout << answer << std::endl; //The client prints out what it received from the server.

        //If we get an 'ACK 4', aka an acknowledgment to a logout request, we stop the client.
        if (answer == "ACK 4") {
            terminate = true;
            break;
        }
        else if (answer == "ERROR 4") {
            logoutCommand = false;
        }
    }
}

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    connectionHandler = new ConnectionHandler(host, port);
    if (!connectionHandler->connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    thread writer(writeToServer);
    thread reader(readFromServer);
    reader.join();
    writer.join();

    delete connectionHandler;
    return 0;
}
