package bgu.spl.net.impl.BGRSServer;

import bgu.spl.net.impl.BGRSimpl.BGRSMessageEncoderDecoder;
import bgu.spl.net.impl.BGRSimpl.BGRSMessagingProtocol;
import bgu.spl.net.srv.Server;

public class TPCMain {

    public static void main(String[] args){
        Server.threadPerClient(
                Integer.parseInt(args[0]), //port
                BGRSMessagingProtocol::new, //protocol factory
                BGRSMessageEncoderDecoder::new //message encoder decoder factory
        ).serve();
    }

}
