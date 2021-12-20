package bgu.spl.net.impl.BGRSimpl;

import bgu.spl.net.Database;
import bgu.spl.net.api.MessagingProtocol;

public class BGRSMessagingProtocol implements MessagingProtocol<String> {
    private boolean shouldTerminate = false;
    private Database db = Database.getInstance();

    private String username = ""; //We save the username of the logged in user
    private boolean isStudent = false; //Also save whether or not it's a student/admin

    //Every message has a private function to convert it to an ack/err msg
    @Override
    public String process(String msg) {
        String[] msgParts = msg.split(" ");

        if (msg.startsWith("ADMINREG") & username.equals("")){
            return AdminReg(msgParts);
        }
        else if (msg.startsWith("STUDENTREG") & username.equals("")){
            return StudentReg(msgParts);
        }
        else if (msg.startsWith("LOGIN") & username.equals("")){
            return Login(msgParts);
        }
        else if (msg.startsWith("LOGOUT") & !username.equals("")){
            return Logout(msgParts);
        }
        else if (msg.startsWith("COURSEREG") & !username.equals("") & isStudent){
            return CourseReg(msgParts);
        }
        else if (msg.startsWith("KDAMCHECK") & !username.equals("") & isStudent){
            return KdamCheck(msgParts);
        }
        else if (msg.startsWith("COURSESTAT") & !username.equals("") & !isStudent){
            return CourseStat(msgParts);
        }
        else if (msg.startsWith("STUDENTSTAT") & !username.equals("") & !isStudent){
            return StudentStat(msgParts);
        }
        else if (msg.startsWith("ISREGISTERED") & !username.equals("") & isStudent){
            return IsRegistered(msgParts);
        }
        else if (msg.startsWith("UNREGISTER") & !username.equals("") & isStudent){
            return Unregister(msgParts);
        }
        else if (msg.startsWith("MYCOURSES") & !username.equals("") & isStudent){
            return MyCourses(msgParts);
        }

        return "ERR " + commandToOpcode(msgParts[0]);
    }

    private String AdminReg(String[] msgParts){
        try{
            db.adminReg(msgParts[1], msgParts[2]);
            return "ACK " + commandToOpcode(msgParts[0]);
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String StudentReg(String[] msgParts){
        try{
            db.studentReg(msgParts[1], msgParts[2]);
            return "ACK " + commandToOpcode(msgParts[0]);
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String Login(String[] msgParts){
        if (db.checkStudentLogin(msgParts[1], msgParts[2])){
            isStudent = true;
            username = msgParts[1];
            return "ACK " + commandToOpcode(msgParts[0]);
        }
        else if (db.checkAdminLogin(msgParts[1], msgParts[2])){
            isStudent = false;
            username = msgParts[1];
            return "ACK " + commandToOpcode(msgParts[0]);
        }
        else{
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String Logout(String[] msgParts){
        db.Logout(username);
        shouldTerminate = true;
        return "ACK " + commandToOpcode(msgParts[0]);
    }

    private String CourseReg(String[] msgParts){
        try{
            db.studentRegCourse(username, Integer.parseInt(msgParts[1]));
            return "ACK " + commandToOpcode(msgParts[0]);
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String KdamCheck(String[] msgParts){
        try{
            String kdams = db.kdamCheck(Integer.parseInt(msgParts[1]));
            return "ACK " + commandToOpcode(msgParts[0]) + "\n" + kdams;
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String CourseStat(String[] msgParts){
        try{
            String[] data = db.courseStat(Integer.parseInt(msgParts[1]));
            return "ACK " + commandToOpcode(msgParts[0]) + "\n" + "Course: (" + msgParts[1] + ") " + data[0] + "\n" + "Seats Available: " + data[1] + "\n" + "Students Registered: " + data[2];
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String StudentStat(String[] msgParts){
        try{
            String data = db.studentStat(msgParts[1]);
            return "ACK " + commandToOpcode(msgParts[0]) + "\n" + "Student: " + msgParts[1] + "\n" + "Courses: " + data;
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String IsRegistered(String[] msgParts){
        try{
            String data = db.isRegistered(username, Integer.parseInt(msgParts[1]));
            return "ACK " + commandToOpcode(msgParts[0]) + "\n" + data;
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String Unregister(String[] msgParts){
        try{
            db.studentUnregCourse(username, Integer.parseInt(msgParts[1]));
            return "ACK " + commandToOpcode(msgParts[0]);
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String MyCourses(String[] msgParts){
        try{
            int[] data = db.studentGetCourses(username);
            String Courses = "[";
            for (int i = 0; i < data.length; i++) {
                Courses = Courses + data[i] + ",";
            }
            if(Courses.length() > 2)
                Courses = Courses.substring(0, Courses.length() - 1);
            Courses = Courses + "]";

            return "ACK " + commandToOpcode(msgParts[0]) + "\n" + Courses;
        } catch (Exception e){
            return "ERR " + commandToOpcode(msgParts[0]);
        }
    }

    private String commandToOpcode(String command){
        if (command.equals("ADMINREG"))
            return "1";
        else if (command.equals("STUDENTREG"))
            return "2";
        else if (command.equals("LOGIN"))
            return "3";
        else if (command.equals("LOGOUT"))
            return "4";
        else if (command.equals("COURSEREG"))
            return "5";
        else if (command.equals("KDAMCHECK"))
            return "6";
        else if (command.equals("COURSESTAT"))
            return "7";
        else if (command.equals("STUDENTSTAT"))
            return "8";
        else if (command.equals("ISREGISTERED"))
            return "9";
        else if (command.equals("UNREGISTER"))
            return "10";
        else //MYCOURSES
            return "11";
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
