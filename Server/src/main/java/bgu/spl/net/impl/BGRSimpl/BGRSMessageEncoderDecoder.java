package bgu.spl.net.impl.BGRSimpl;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.Arrays;

public class BGRSMessageEncoderDecoder implements MessageEncoderDecoder<String> {
    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
    private int bytesRead = 0;
    private short opcode = 0;
    private int numberOfZeroBits = 0;

    @Override
    public String decodeNextByte(byte nextByte) {
        pushByte(nextByte);

        if (bytesRead == 2){ //We save the opcode after 2 bytes were read
            opcode = bytesToShort(bytes[0], bytes[1]);
        }
        if (nextByte == '\0'){
            numberOfZeroBits++;
        }

        if (numberOfZeroBits == 3 & opcode == 1){
            //adminreg
            return AdminReg_StudentReg_Login("ADMINREG");
        }
        else if (numberOfZeroBits == 3 & opcode == 2){
            //studentreg
            return AdminReg_StudentReg_Login("STUDENTREG");
        }
        else if (numberOfZeroBits == 3 & opcode == 3){
            //login
            return AdminReg_StudentReg_Login("LOGIN");
        }
        else if (numberOfZeroBits == 1 & opcode == 4){
            reset();
            return "LOGOUT";
        }
        else if (opcode == 5 & bytesRead == 4){
            //coursereg
            return CourseReg_KdamCheck_CourseStat_IsRegistered_Unregister("COURSEREG");
        }
        else if (opcode == 6 & bytesRead == 4){
            //kdamcheck
            return CourseReg_KdamCheck_CourseStat_IsRegistered_Unregister("KDAMCHECK");
        }
        else if (opcode == 7 & bytesRead == 4){
            //coursestat
            return CourseReg_KdamCheck_CourseStat_IsRegistered_Unregister("COURSESTAT");
        }
        else if (numberOfZeroBits == 2 & opcode == 8){
            //studentstat
            return StudentStat("STUDENTSTAT");
        }
        else if (opcode == 9 & bytesRead == 4){
            //isregistered
            return CourseReg_KdamCheck_CourseStat_IsRegistered_Unregister("ISREGISTERED");
        }
        else if (opcode == 10 & bytesRead == 4){
            //unregister
            return CourseReg_KdamCheck_CourseStat_IsRegistered_Unregister("UNREGISTER");
        }
        else if (opcode == 11){
            reset();
            return "MYCOURSES";
        }

        return null;
    }

    //This function resets the data of the encdec after each msg, so the next msg will be received and decoded from scratch
    private void reset(){
        bytes = new byte[1 << 10];
        len = 0;
        bytesRead = 0;
        opcode = 0;
        numberOfZeroBits = 0;
    }

    //This function is responsible for ADMINREG / STUDENTREG / LOGIN
    private String AdminReg_StudentReg_Login(String operation){
        String msg = operation + " ";

        int index = 2;
        for (index = 2; index < bytesRead & bytes[index] != '\0'; index++) {
            msg = msg + (char)bytes[index];
        }

        msg = msg + " ";

        for (index = index + 1; index < bytesRead; index++) {
            msg = msg + (char)bytes[index];
        }

        reset();

        return msg;
    }

    //This function is responsible for COURSEREG / KDAMCHECK / COURSESTAT / ISREGISTERED / UNREGISTER
    private String CourseReg_KdamCheck_CourseStat_IsRegistered_Unregister(String operation){
        String msg = operation + " ";

        short courseNumber = bytesToShort(bytes[2], bytes[3]);

        msg = msg + courseNumber;

        reset();

        return msg;
    }

    //This function is responsible for STUDENTSTAT
    private String StudentStat(String operation){
        String msg = operation + " ";

        int index = 2;
        for (index = 2; index < bytesRead & bytes[index] != '\0'; index++) {
            msg = msg + (char)bytes[index];
        }

        reset();

        return msg;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
        bytesRead++;
    }

    private short bytesToShort(byte byte1, byte byte2)
    {
        short result = (short)((byte1 & 0xff) << 8);
        result += (short)(byte2 & 0xff);
        return result;
    }

    @Override
    public byte[] encode(String message) {
        if (message.startsWith("ERR")) {
            return errorTranslation(message);
        }
        else {
            return ackTranslation(message);
        }
    }

    private byte[] errorTranslation(String message){
        byte[] errorAsBytes = new byte[4];
        short opcode = 13;
        byte[] temp = shortToBytes(opcode);
        errorAsBytes[0] = temp[0];
        errorAsBytes[1] = temp[1];

        short messageOpcode = Short.parseShort(message.split(" ")[1]);
        temp = shortToBytes(messageOpcode);
        errorAsBytes[2] = temp[0];
        errorAsBytes[3] = temp[1];

        return errorAsBytes;
    }

    private byte[] ackTranslation(String message){
        String findOpCode;
        if(message.length() > 5 && Character.isDigit(message.charAt(5))){
            findOpCode = message.substring(4, 6);
        }
        else{
            findOpCode = "" + message.charAt(4);
        }

        //ack opcode
        byte[] ackAsBytes = new byte[message.length() - findOpCode.length() + 1];
        short opcode = 12;
        byte[] temp = shortToBytes(opcode);
        ackAsBytes[0] = temp[0];
        ackAsBytes[1] = temp[1];

        //msg opcode
        short messageOpcode = Short.parseShort(findOpCode);
        temp = shortToBytes(messageOpcode);
        ackAsBytes[2] = temp[0];
        ackAsBytes[3] = temp[1];

        //if the msg has extra information we add it here
        if(messageOpcode == 7 | messageOpcode == 8 | messageOpcode == 9 | messageOpcode == 11 | messageOpcode == 6){
            int indexOfArray = 4;
            for (int indexOfMessage = 4 + findOpCode.length(); indexOfMessage < message.length(); indexOfMessage++) {
                short ch = (short)message.charAt(indexOfMessage);
                ackAsBytes[indexOfArray] = shortToBytes(ch)[1];
                indexOfArray++;
            }
        }

        //\0 byte at the end of the msg
        ackAsBytes[ackAsBytes.length - 1] = '\0';

        return ackAsBytes;
    }

    private byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }
}
