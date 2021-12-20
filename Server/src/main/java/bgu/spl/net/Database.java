package bgu.spl.net;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passive object representing the Database where all courses and users are stored.
 * <p>
 * This class must be implemented safely as a thread-safe singleton.
 * You must not alter any of the given public methods of this class.
 * <p>
 * You can add private fields and methods to this class as you see fit.
 */
public class Database {
    private List<Integer> CoursesOrder;
    private HashMap<Integer, String[]> Courses;
    private ConcurrentHashMap<String, String> Admins;
    private ConcurrentHashMap<String, String[]> Students;
    private ConcurrentHashMap<Integer, List<String>> StudentsInCourses;
    private List<String> loggedIn;

    //to prevent user from creating new Database
    private Database() {
        CoursesOrder = new ArrayList<>();
        Courses = new HashMap<>();
        Admins = new ConcurrentHashMap<>();
        Students = new ConcurrentHashMap<>();
        StudentsInCourses = new ConcurrentHashMap<>();
        loggedIn = new ArrayList<>();

        initialize("Courses.txt");
    }

    private static class SingletonClass {
        private static final Database instance = new Database();
    }

    /**
     * Retrieves the single instance of this class.
     */
    public static Database getInstance() {
        return SingletonClass.instance;
    }

    /**
     * loads the courses from the file path specified
     * into the Database, returns true if successful.
     */
    boolean initialize(String coursesFilePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(coursesFilePath));

            for (String line : lines) {
                String courseID = returnNextData(line);
                line = line.substring(line.indexOf("|") + 1);
                String courseName = returnNextData(line);
                line = line.substring(line.indexOf("|") + 1);
                String courseKdams = returnNextData(line);
                line = line.substring(line.indexOf("|") + 1);
                String courseCapacity = line;

                /**
                 * Courses are stored as such:
                 * courseID as the key
                 * An array of strings as the value, in the array are stored:
                 * array[0] = courseName
                 * array[1] = courseKdams
                 * array[2] = courseCapacity
                 * array[3] = course available seats
                 */
                Courses.put(Integer.parseInt(courseID), new String[]{courseName, courseKdams, courseCapacity, courseCapacity});

                CoursesOrder.add(Integer.parseInt(courseID));

                List<String> newList = new ArrayList<>();
                StudentsInCourses.put(Integer.parseInt(courseID), newList);
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String returnNextData(String line) {
        int index = line.indexOf("|");
        String Data = line.substring(0, index);
        return Data;
    }

    public String getCourseName(int courseID) {
        return Courses.get(courseID)[0];
    }

    public int[] getCourseKdams(int courseID) {
        String asArray = Courses.get(courseID)[1];
        if (asArray.length() != 2) { //If the string is more than just [] we convert it to an array
            return Arrays.stream(asArray.substring(1, asArray.length() - 1).split(","))
                    .map(String::trim).mapToInt(Integer::parseInt).toArray();
        } else //else we return an empty array
            return new int[0];
    }

    public int getCourseCapacity(int courseID) {
        return Integer.parseInt(Courses.get(courseID)[2]);
    }

    public int getCourseSeatsAvailable(int courseID) {
        return Integer.parseInt(Courses.get(courseID)[3]);
    }

    public void adminReg(String username, String password) throws Exception {
        if (Admins.containsKey(username))
            throw new Exception("An admin with the username " + username + " is already registered to the system.");

        Admins.put(username, password);
    }

    public synchronized boolean checkAdminLogin(String username, String password) {
        try {
            if (!Admins.containsKey(username))
                throw new Exception("An admin with the username " + username + " is not registered to the system.");
            if (!Admins.get(username).equals(password))
                throw new Exception("The password " + password + " is not the correct password for " + username + ".");
            if (loggedIn.contains(username))
                throw new Exception("This user is already logged in.");
            loggedIn.add(username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void Logout(String username) {
        loggedIn.remove(username);
    }

    public void studentReg(String username, String password) throws Exception {
        if (Students.containsKey(username))
            throw new Exception("A student with the username " + username + " is already registered to the system.");

        Students.put(username, new String[]{password, "[]"});
    }

    public synchronized boolean checkStudentLogin(String username, String password) {
        try {
            if (!Students.containsKey(username))
                throw new Exception("A student with the username " + username + " is not registered to the system.");
            if (!Students.get(username)[0].equals(password))
                throw new Exception("The password " + password + " is not the correct password for " + username + ".");
            if (loggedIn.contains(username))
                throw new Exception("This user is already logged in.");
            loggedIn.add(username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int[] studentGetCourses(String username) {
        String coursesString = Students.get(username)[1];
        int[] studentCoursesNotSorted;
        if (coursesString.length() != 2) {
            studentCoursesNotSorted = Arrays.stream(coursesString.substring(1, coursesString.length() - 1).split(","))
                    .map(String::trim).mapToInt(Integer::parseInt).toArray();
        } else
            studentCoursesNotSorted = new int[0];

        int[] studentCoursesSorted = new int[studentCoursesNotSorted.length];
        int index = 0;
        for (Integer id : CoursesOrder) {
            if (Arrays.stream(studentCoursesNotSorted).anyMatch(i -> i == id)) {
                studentCoursesSorted[index] = id;
                index++;
            }
        }

        return studentCoursesSorted;
    }

    public synchronized void studentRegCourse(String username, int courseID) throws Exception {
        if (!Courses.containsKey(courseID))
            throw new Exception("The course " + courseID + " does not exist in the database.");
        if (getCourseSeatsAvailable(courseID) == 0)
            throw new Exception("The course " + courseID + " has no available seats left.");
        if (Arrays.stream(studentGetCourses(username)).anyMatch(i -> i == courseID))
            throw new Exception("The student " + username + " is already registered to the course " + courseID + ".");
        for (int kdam : getCourseKdams(courseID)) {
            if (Arrays.stream(studentGetCourses(username)).noneMatch(i -> i == kdam))
                throw new Exception("The student " + username + " isn't registered to all kdam courses for " + courseID + ". One of the required kdams is " + courseID + ".");
        }

        String pass = Students.get(username)[0];
        String courses = Students.get(username)[1];

        courses = courses.substring(0, courses.length() - 1); //Removing ]
        if (courses.length() != 1) //adding the course to the end of the string
            courses = courses + "," + courseID + "]";
        else
            courses = courses + courseID + "]";

        Students.replace(username, new String[]{pass, courses});

        int oldCap = getCourseCapacity(courseID);
        int newSeatsTaken = getCourseSeatsAvailable(courseID) - 1; //Removing 1 from the available seats
        String oldName = getCourseName(courseID);
        String oldKdams = Courses.get(courseID)[1];

        Courses.replace(courseID, new String[]{oldName, oldKdams, oldCap + "", newSeatsTaken + ""}); //updating Courses

        StudentsInCourses.get(courseID).add(username);

    }

    public String kdamCheck(int courseID) throws Exception {
        if (!Courses.containsKey(courseID))
            throw new Exception("The course " + courseID + " is not in the system.");

        String kdams = Courses.get(courseID)[1];
        int[] kdamsArrayNotSorted;
        if (kdams.length() != 2) {
            kdamsArrayNotSorted = Arrays.stream(kdams.substring(1, kdams.length() - 1).split(","))
                    .map(String::trim).mapToInt(Integer::parseInt).toArray();
        } else
            kdamsArrayNotSorted = new int[0];

        if (kdamsArrayNotSorted.length == 0)
            return "[]";

        int[] kdamsSorted = new int[kdamsArrayNotSorted.length];
        int index = 0;
        for (Integer id : CoursesOrder) {
            if (Arrays.stream(kdamsArrayNotSorted).anyMatch(i -> i == id)) {
                kdamsSorted[index] = id;
                index++;
            }
        }

        String kdamsSortedString = Arrays.toString(kdamsSorted);
        kdamsSortedString = kdamsSortedString.replace(", ", ",");

        return kdamsSortedString;
    }

    public String isRegistered(String username, int courseID) {
        if (Students.get(username)[1].contains(courseID + ""))
            return "REGISTERED";
        return "NOT REGISTERED";
    }

    public String[] courseStat(int courseID) throws Exception {
        if (!Courses.containsKey(courseID))
            throw new Exception("The course " + courseID + " is not in the system.");

        String courseName = getCourseName(courseID);
        String capacity = getCourseSeatsAvailable(courseID) + "/" + getCourseCapacity(courseID);

        List<String> studentsInCourse = StudentsInCourses.get(courseID);
        Collections.sort(studentsInCourse);

        String students = "[";
        for (String student : studentsInCourse) {
            students = students + student + ",";
        }
        if (students.length() > 2)
            students = students.substring(0, students.length() - 1);
        students = students + "]";

        return new String[]{courseName, capacity, students}; //returning an array to be parsed later
    }

    public String studentStat(String username) throws Exception {
        if (!Students.containsKey(username))
            throw new Exception("The student " + username + " is not registered to the system.");

        int[] CoursesArray = studentGetCourses(username);

        String Courses = "[";
        for (int i = 0; i < CoursesArray.length; i++) {
            Courses = Courses + CoursesArray[i] + ",";
        }

        if (Courses.length() > 2)
            Courses = Courses.substring(0, Courses.length() - 1);
        Courses = Courses + "]";

        return Courses;
    }

    public synchronized void studentUnregCourse(String username, int courseID) throws Exception {
        if (!Students.containsKey(username))
            throw new Exception("The student " + username + " is not registered to the system.");
        if (!StudentsInCourses.get(courseID).contains(username))
            throw new Exception("The student " + username + " is not registered to the course " + courseID + ".");

        //Removing from StudentsInCourses
        StudentsInCourses.get(courseID).remove(username);

        //Removing from Courses
        String oldname = getCourseName(courseID);
        String oldkdams = Courses.get(courseID)[1];
        String oldcapacity = Courses.get(courseID)[2];
        int newSeatsTaken = getCourseSeatsAvailable(courseID) + 1; //Adding 1 to the available seats
        Courses.replace(courseID, new String[]{oldname, oldkdams, oldcapacity, newSeatsTaken + ""}); //updating Courses

        //Removing from Students
        String oldpass = Students.get(username)[0];
        int[] oldcourses = studentGetCourses(username);
        int[] newcourses = new int[oldcourses.length - 1];
        int index = 0;
        for (int i = 0; i < oldcourses.length; i++) {
            if (oldcourses[i] != courseID) {
                newcourses[index] = oldcourses[i];
                index++;
            }
        }
        String newcoursesString = Arrays.toString(newcourses);
        newcoursesString = newcoursesString.replace(", ", ",");
        Students.replace(username, new String[]{oldpass, newcoursesString});
    }

}
