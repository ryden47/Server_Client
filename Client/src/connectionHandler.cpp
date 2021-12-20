#include <connectionHandler.h>
 
using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
 
ConnectionHandler::ConnectionHandler(string host, short port): host_(host), port_(port), io_service_(), socket_(io_service_), bytesToSend(0){}
    
ConnectionHandler::~ConnectionHandler() {
    close();
}
 
bool ConnectionHandler::connect() {
    std::cout << "Starting connect to " 
        << host_ << ":" << port_ << std::endl;
    try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
		boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error)
			throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
 
bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
			tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed1 (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[]) {
    int tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToSend > tmp ) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToSend - tmp), error);
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        delete[] bytes;
        return false;
    }
    delete[] bytes;
    return true;
}

bool ConnectionHandler::getLine(std::string& line) {
    return getFrameAscii(line, '\0');
}

bool ConnectionHandler::sendLine(std::string& line) {
    return sendFrameAscii(line);
}
 

bool ConnectionHandler::getFrameAscii(std::string& frame, char delimiter) {
    char ch;
    try {
        if(!getBytes(&ch, 2)) //first get the bytes of ack/err
        {
            return false;
        }
        short opcode = bytesToShort(&ch);
        if(opcode == 12){ //ACK
            frame = "ACK ";

            if(!getBytes(&ch, 2)) //get the bytes of opcode command
            {
                return false;
            }
            short messageopcode = bytesToShort(&ch);
            frame = frame + std::to_string(messageopcode); //msg to print
            do { //get extra information from the socket if exists
                getBytes(&ch, 1);
                if (ch != delimiter)
                    frame.append(1, ch);
            } while (delimiter != ch);
        }
        else{ //ERR
            getBytes(&ch, 2); //get the bytes of opcode command
            short messageopcode = bytesToShort(&ch);
            frame = "ERROR " + std::to_string(messageopcode); //msg to print
        }
    } catch (std::exception& e) {
	std::cerr << "recv failed2 (Error: " << e.what() << ')' << std::endl;
	return false;
    }
    return true;
}
 
 
bool ConnectionHandler::sendFrameAscii(const std::string& frame) {
	bool result=sendBytes(convertToBytes(frame));
	return result;
}
 
// Close down the connection properly.
void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}

// Each command has its own private function to convert the string to an array of bytes
// We return the arrays and later send them to sendBytes().
char* ConnectionHandler::convertToBytes(const string &frame) {
    if(frame.length() > 8 && frame.substr(0, 8) == "ADMINREG"){
        return convertSTUDENTREG(frame);
    }
    else if(frame.length() > 10 && frame.substr(0, 10) == "STUDENTREG"){
        return convertADMINREG(frame);
    }
    else if(frame.length() > 5 && frame.substr(0, 5) == "LOGIN"){
        return convertLOGIN(frame);
    }
    else if(frame == "LOGOUT"){
        return convertLOGOUT(frame);
    }
    else if(frame.length() > 9 && frame.substr(0, 9) == "COURSEREG"){
        return convertCOURSEREG(frame);
    }
    else if(frame.length() > 9 && frame.substr(0, 9) == "KDAMCHECK"){
        return convertKDAMCHECK(frame);
    }
    else if(frame.length() > 10 && frame.substr(0, 10) == "COURSESTAT"){
        return convertCOURSESTAT(frame);
    }
    else if(frame.length() > 11 && frame.substr(0, 11) == "STUDENTSTAT"){
        return convertSTUDENTSTAT(frame);
    }
    else if(frame.length() > 12 && frame.substr(0, 12) == "ISREGISTERED"){
        return convertISREGISTERED(frame);
    }
    else if(frame.length() > 10 && frame.substr(0, 10) == "UNREGISTER"){
        return convertUNREGISTER(frame);
    }
    else if(frame == "MYCOURSES"){
        return convertMYCOURSES(frame);
    }

    return nullptr;
}

void ConnectionHandler::shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

short ConnectionHandler::bytesToShort(char* bytesArr)
{
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

char* ConnectionHandler::convertSTUDENTREG(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];
    int bytesIndex = 2;

    bytes = new char[frame.length() - 8 + 2]; // 8 for ADMINREG 2 for opcode
    bytesToSend = frame.length() - 8 + 2;
    shortToBytes(1, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    for (unsigned int i = 9; i < frame.length(); ++i) {
        if(frame.at(i) == ' '){
            bytes[bytesIndex++] = '\0';
        } else {
            bytes[bytesIndex++] = frame.at(i);
        }
    }
    bytes[bytesIndex] = '\0';
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertADMINREG(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];
    int bytesIndex = 2;

    bytes = new char[frame.length() - 10 + 2]; // 10 for STUDENTREG 2 for opcode
    bytesToSend = frame.length() - 10 + 2;
    shortToBytes(2, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    for (unsigned int i = 11; i < frame.length(); ++i) {
        if(frame.at(i) == ' '){
            bytes[bytesIndex++] = '\0';
        } else {
            bytes[bytesIndex++] = frame.at(i);
        }
    }
    bytes[bytesIndex] = '\0';
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertLOGIN(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];
    int bytesIndex = 2;

    bytes = new char[frame.length() - 5 + 2]; // 5 for LOGIN 2 for opcode
    bytesToSend = frame.length() - 5 + 2;
    shortToBytes(3, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    for (unsigned int i = 6; i < frame.length(); ++i) {
        if(frame.at(i) == ' '){
            bytes[bytesIndex++] = '\0';
        } else {
            bytes[bytesIndex++] = frame.at(i);
        }
    }
    bytes[bytesIndex] = '\0';
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertLOGOUT(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];

    bytes = new char[2];
    bytesToSend = 2;
    shortToBytes(4, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertCOURSEREG(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];

    bytes = new char[4];
    bytesToSend = 4;
    shortToBytes(5, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    string number = frame.substr(10);
    short course = std::stoi(number);
    char* numbersBytes = new char[2];
    shortToBytes(course, numbersBytes);
    bytes[2] = numbersBytes[0];
    bytes[3] = numbersBytes[1];
    delete[] numbersBytes;
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertKDAMCHECK(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];

    bytes = new char[4];
    bytesToSend = 4;
    shortToBytes(6, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    string number = frame.substr(10);
    short course = std::stoi(number);
    char* numbersBytes = new char[2];
    shortToBytes(course, numbersBytes);
    bytes[2] = numbersBytes[0];
    bytes[3] = numbersBytes[1];
    delete[] numbersBytes;
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertCOURSESTAT(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];

    bytes = new char[4];
    bytesToSend = 4;
    shortToBytes(7, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    string number = frame.substr(11);
    short course = std::stoi(number);
    char* numbersBytes = new char[2];
    shortToBytes(course, numbersBytes);
    bytes[2] = numbersBytes[0];
    bytes[3] = numbersBytes[1];
    delete[] numbersBytes;
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertSTUDENTSTAT(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];
    int bytesIndex = 2;

    bytes = new char[frame.length() - 11 + 2]; // 11 for STUDENTSTAT 2 for opcode
    bytesToSend = frame.length() - 11 + 2;
    shortToBytes(8, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    for (unsigned int i = 12; i < frame.length(); ++i) {
        if(frame.at(i) == ' '){
            bytes[bytesIndex++] = '\0';
        } else {
            bytes[bytesIndex++] = frame.at(i);
        }
    }
    bytes[bytesIndex] = '\0';
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertISREGISTERED(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];

    bytes = new char[4];
    bytesToSend = 4;
    shortToBytes(9, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    string number = frame.substr(13);
    short course = std::stoi(number);
    char* numbersBytes = new char[2];
    shortToBytes(course, numbersBytes);
    bytes[2] = numbersBytes[0];
    bytes[3] = numbersBytes[1];
    delete[] numbersBytes;
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertUNREGISTER(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];

    bytes = new char[4];
    bytesToSend = 4;
    shortToBytes(10, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    string number = frame.substr(11);
    short course = std::stoi(number);
    char* numbersBytes = new char[2];
    shortToBytes(course, numbersBytes);
    bytes[2] = numbersBytes[0];
    bytes[3] = numbersBytes[1];
    delete[] numbersBytes;
    delete[] opbytes;
    return bytes;
}

char *ConnectionHandler::convertMYCOURSES(const string &frame) {
    char* bytes;
    char* opbytes = new char[2];

    bytes = new char[2];
    bytesToSend = 2;
    shortToBytes(11, opbytes);
    bytes[0] = opbytes[0];
    bytes[1] = opbytes[1];
    delete[] opbytes;
    return bytes;
}