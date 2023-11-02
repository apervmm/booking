import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;

class InnReservations {
    public static void main(String args[]) throws SQLException {
        InnReservations g = new InnReservations();
        g.userOptions();
    }

    private static void userOptions() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        String userInput = "";
        while (!(userInput.equals("quit"))) {
            System.out.println(
                    "Here are the options: \n [1] - Reservation\n [2] - Adding Reservation\n [3] - Reservation Change\n [4] - Reservation Cancellation");
            System.out.println(" [5] - Detailed Reservation Information\n [6] - Revenue\n [7] - Quit");
            System.out.println("What option [#] do you want: ");
            userInput = scanner.nextLine();
            if (userInput.equals("1")) {
                getRoomsAndRates();
            } else if (userInput.equals("2")) {
                getReservations(scanner);
            } else if (userInput.equals("3")) {
                getReservationChange(scanner);
            } else if (userInput.equals("4")) {
                getReservationCancellation(scanner);
            } else if (userInput.equals("5")) {
                getReservationInformation(scanner);
            } else if (userInput.equals("6")) {
                getRevenue();
            } else if (userInput.equals("7")) {
                userInput = "quit";
            }
        }
    }

    // FRS 1:
    private static void getRoomsAndRates() throws SQLException {
        String result = "";
        String sqlStatement = " with A as ( select Room, SUM(DateDiff(Checkout,\n" +
                "case when CheckIn >=  CurDate() - interval 360 day then CheckIn else CurDate() - interval 360 day end)) as DaysOccupied\n"
                +
                "from anguy714.lab7_reservations join anguy714.lab7_rooms on Room = RoomCode where CheckOut > CurDate() - interval 360 day group by Room),\n"
                +
                "B as ( select Room, MAX(CheckIn) as MostRecentCheckin, MAX(Checkout) as MostRecentCheckout from anguy714.lab7_reservations group by Room) \n"
                +
                "select B.Room, ROUND(DaysOccupied / 180, 2) as Popularity, DATE_ADD(MostRecentCheckout, interval 1 day) as FirstAvailable, DATEDIFF(MostRecentCheckout,MostRecentCheckin) as LastStayLength, MostRecentCheckout\n"
                +
                "from B join A on A.Room = B.Room order by Popularity desc";

        driverCheck();

        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            Statement query = conn.createStatement();
            // Exucution of query
            ResultSet rs = query.executeQuery(sqlStatement);

            while (rs.next()) {
                String roomSql = rs.getString("Room");
                Float popolaritySql = rs.getFloat("Popularity");
                Date firstAvailableSql = rs.getDate("FirstAvailable");
                int lastStayLengthSql = rs.getInt("LastStayLength");
                Date mostRecentCheckoutSql = rs.getDate("MostRecentCheckout");

                result += String.format(
                        "\nRoom: %s\nPopularity: %.2f\nFirst Available: %tF\nLast Stay Length: %d\nMost Recent Checkout: %tF\n",
                        roomSql, popolaritySql, firstAvailableSql, lastStayLengthSql, mostRecentCheckoutSql);
            }
            System.out.println("Res " + result);

        } catch (SQLException se) {
            System.out.println("Query Failed");
        }
    }

    // FRS 2:
    private static void getReservations(Scanner scanner) throws SQLException {

        driverCheck();

        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            String FirstName = "";
            String LastName = "";
            String RoomCode = "";
            String BedType = "";
            String CheckIn = "";
            String CheckOut = "";
            String Kids = "";
            String Adults = "";
            String result = "";
            String roomChoice = "";
            String choice = "";
            Boolean validInput = false;
            Boolean anyRoom = false;
            Boolean anyBed = false;

            // Get info -- assume correct inputs for now
            System.out.println("Please provide the following information to make a reservation: ");
            // Will need a cancel option
            System.out.println("First Name: ");
            FirstName = scanner.nextLine();
            System.out.println("Last Name: ");
            LastName = scanner.nextLine();
            System.out.println("Room Code (Input 'Any' for no preference): ");
            RoomCode = scanner.nextLine();
            System.out.println("Bed Type (Input 'Any' for no preference): ");
            BedType = scanner.nextLine();
            System.out.println("Check In Date [year-month-day]: ");
            CheckIn = scanner.nextLine();
            System.out.println("Check Out Date [year-month-day]: ");
            CheckOut = scanner.nextLine();
            System.out.println("Amount of Children: ");
            Kids = scanner.nextLine();
            System.out.println("Amount of Adults: ");
            Adults = scanner.nextLine();
            // if successfull - output all the information

            String sqlStatement = "SELECT * \nFROM anguy714.lab7_rooms\nWHERE maxOcc >= ( ? + ? ) \nAND " +
                    "RoomCode NOT IN (SELECT DISTINCT RoomCode\nFROM anguy714.lab7_rooms INNER JOIN anguy714.lab7_reservations ON RoomCode = Room\n"
                    +
                    "WHERE CheckIn <= ? AND CheckOut >= ?)";
            // Handle any
            if (!"any".equalsIgnoreCase(RoomCode)) {
                sqlStatement = sqlStatement + " AND RoomCode = ?";
            } else {
                anyRoom = true;
            }
            if (!"any".equalsIgnoreCase(BedType)) {
                sqlStatement = sqlStatement + " AND BedType = ?";
            } else {
                anyBed = true;
            }

            validInput = true;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlStatement);) {
                pstmt.setInt(1, Integer.parseInt(Adults));
                pstmt.setInt(2, Integer.parseInt(Kids));
                pstmt.setDate(3, java.sql.Date.valueOf(CheckOut));
                pstmt.setDate(4, java.sql.Date.valueOf(CheckIn));
                if (!anyRoom && anyBed) {
                    pstmt.setString(5, RoomCode);
                } else if (!anyBed && anyRoom) {
                    pstmt.setString(5, BedType);
                } else if (!anyRoom && !anyBed) {
                    pstmt.setString(5, RoomCode);
                    pstmt.setString(6, BedType);
                }
                // Keep track of rooms available
                Map<Integer, List<String>> roomMap = new HashMap<Integer, List<String>>();

                System.out.println("Below are the available rooms and associated information for each:");
                Integer roomcount = 1;
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    List<String> roomInfo = new ArrayList<String>();
                    String roomcodeSql = rs.getString("RoomCode");
                    String roomnameSql = rs.getString("RoomName");
                    String bedtypeSql = rs.getString("bedType");
                    int maxoccSql = rs.getInt("maxOcc");
                    int baseRateSql = rs.getInt("basePrice");

                    roomInfo.add(roomcodeSql);
                    roomInfo.add(roomnameSql);
                    roomInfo.add(bedtypeSql);
                    roomInfo.add(Integer.toString(maxoccSql));
                    roomInfo.add(Integer.toString(baseRateSql));
                    roomMap.put(roomcount, roomInfo);

                    System.out.format("Room [%d]\nRoomCode: %s\nRoomName: %s\nbedType: %s\nmaxOcc: %d\n", roomcount,
                            roomcodeSql, roomnameSql, bedtypeSql, maxoccSql);
                    roomcount += 1;

                }
                if (roomMap.isEmpty()) {
                    System.out
                            .println("We apologize, there does not appear to be any room that fits your preferences.");
                } else {

                    System.out.println(
                            "Which room [#] would you like to reserve? Or type 'Cancel' to go back to the main menu.");
                    roomChoice = scanner.nextLine();

                    if (!"cancel".equalsIgnoreCase(roomChoice)) {
                        // Calculate total cost
                        List<String> roomInfo2 = roomMap.get(Integer.parseInt(roomChoice));
                        LocalDate CheckInDate = LocalDate.parse(CheckIn);
                        LocalDate CheckOutDate = LocalDate.parse(CheckOut);
                        Float totalCost = 0f;
                        Integer base = Integer.parseInt(roomInfo2.get(4));
                        Integer weekday = 0;
                        Integer weekend = 0;
                        Integer day = 0;
                        while (CheckInDate.compareTo(CheckOutDate) != 0) {
                            CheckInDate = CheckInDate.plusDays(1);
                            day = CheckInDate.getDayOfWeek().getValue();
                            if (day == 6 || day == 7)
                                weekend += 1;
                            else
                                weekday += 1;

                        }
                        totalCost = (float) ((weekend * base * 1.1) + (weekday * base));

                        System.out.format("Name: %s %s\nRoom Info: %s (Code), %s (Name), %s (Bed)\n", FirstName,
                                LastName,
                                roomInfo2.get(0), roomInfo2.get(1), roomInfo2.get(2));
                        System.out.format("Adults: %s\nKids: %s\nTotal Cost: %.2f\n", Adults, Kids, totalCost);
                    } else if ("cancel".equalsIgnoreCase(roomChoice)) {
                        return;
                    }
                    List<String> roomInfo2 = roomMap.get(Integer.parseInt(roomChoice));
                    System.out.println(
                            "Type 'Confirm' to confirm your reservation or 'Cancel' to return to the main menu.");
                    choice = scanner.nextLine();
                    if ("confirm".equalsIgnoreCase(roomChoice)) {
                        Random rand = new Random();
                        Integer randint = 20000 + rand.nextInt(10000);
                        String insertsql = "INSERT INTO anguy714.lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
                        try (PreparedStatement psmt2 = conn.prepareStatement(insertsql)) {

                            psmt2.setString(1, Integer.toString(randint));
                            psmt2.setString(2, roomInfo2.get(0));
                            psmt2.setString(3, CheckIn);
                            psmt2.setString(4, CheckOut);
                            psmt2.setString(5, roomInfo2.get(4));
                            psmt2.setString(6, LastName);
                            psmt2.setString(7, FirstName);
                            psmt2.setString(8, Adults);
                            psmt2.setString(9, Kids);

                            psmt2.executeUpdate();

                        } catch (SQLException e) {
                            System.out.println("Rollback" + e);
                            conn.rollback();
                        }
                    } else if ("cancel".equalsIgnoreCase(roomChoice)) {
                        return;
                    }
                }

            } catch (SQLException se) {
                System.out.println("Query Execution Failed");
            }
        } catch (SQLException se) {
            System.out.println("Query Connection Failed");
        }
    }

    // FRS 3:
    private static void getReservationChange(Scanner scanner) throws SQLException {
        driverCheck();
        int ReservationCode = 0;
        String userInput = "";
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            System.out.println("Please enter the reservation code for the reservation you want to change:");
            ReservationCode = Integer.parseInt(scanner.nextLine());
            while (!userInput.equals("quit")) {
                String q = "select * from anguy714.lab7_reservations where CODE = ?;";
                System.out.println(
                        "[1] - First Name \n[2] - Last Name \n[3] - Begin Date \n[4] - End Date \n[5] - Number of Children \n[6] - Number of Adults \n[q] - quit");
                System.out.println("What would Like to Change in Reseravetion? (Select Option): ");
                userInput = scanner.nextLine();

                if (userInput.equals("1")) {
                    String sqlFirstName = "update anguy714.lab7_reservations\n" +
                            "set FirstName = ?\n" +
                            "where CODE = ?;";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlFirstName);) {
                        System.out.println("Enter new First Name: ");
                        String newName = "";
                        newName = scanner.nextLine();
                        stmt.setString(1, newName);
                        stmt.setInt(2, ReservationCode);
                        int rsUpdate = stmt.executeUpdate();
                    } catch (SQLException se) {
                        System.out.println("Query Execution Failed");
                    }
                } else if (userInput.equals("2")) {
                    String sqlLastName = "update anguy714.lab7_reservations\n" +
                            "set LastName = ?\n" +
                            "where CODE = ?;";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlLastName);) {
                        System.out.println("Enter new Last Name: ");
                        String newName = "";
                        newName = scanner.nextLine();
                        stmt.setString(1, newName);
                        stmt.setInt(2, ReservationCode);
                        int rsUpdate = stmt.executeUpdate();
                    } catch (SQLException se) {
                        System.out.println("Query Execution Failed");
                    }
                } else if (userInput.equals("3")) {
                    String sqlquery = "update anguy714.lab7_reservations\n" +
                            "set Checkin = ?\n" +
                            "where CODE = ?;";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlquery);) {
                        System.out.println("Enter new BeginDate [year-month-day]: ");
                        String newName = "";
                        newName = scanner.nextLine();
                        stmt.setDate(1, java.sql.Date.valueOf(newName));
                        stmt.setInt(2, ReservationCode);
                        int rsUpdate = stmt.executeUpdate();
                    } catch (SQLException se) {
                        System.out.println("Query Execution Failed");
                    }
                } else if (userInput.equals("4")) {
                    String sqlquery = "update anguy714.lab7_reservations\n" +
                            "set Checkout = ?\n" +
                            "where CODE = ?;";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlquery);) {
                        System.out.println("Enter new End Date [year-month-day]: ");
                        String newName = scanner.nextLine();
                        stmt.setDate(1, java.sql.Date.valueOf(newName));
                        stmt.setInt(2, ReservationCode);
                        int rsUpdate = stmt.executeUpdate();
                    } catch (SQLException se) {
                        System.out.println("Query Execution Failed");
                    }
                } else if (userInput.equals("5")) {
                    String sqlquery = "update anguy714.lab7_reservations\n" +
                            "set Kids = ?\n" +
                            "where CODE = ?;";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlquery);) {
                        System.out.println("Enter new #Kids: ");
                        int newName = scanner.nextInt();
                        stmt.setInt(1, newName);
                        stmt.setInt(2, ReservationCode);
                        int rsUpdate = stmt.executeUpdate();
                    } catch (SQLException se) {
                        System.out.println("Query Execution Failed");
                    }
                } else if (userInput.equals("6")) {
                    String sqlquery = "update anguy714.lab7_reservations\n" +
                            "set Adults = ?\n" +
                            "where CODE = ?;";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlquery);) {
                        System.out.println("Enter new #Adults: ");
                        int newName = scanner.nextInt();
                        stmt.setInt(1, newName);
                        stmt.setInt(2, ReservationCode);
                        int rsUpdate = stmt.executeUpdate();
                    } catch (SQLException se) {
                        System.out.println("Query Execution Failed");
                    }
                } else if (userInput.equals("q")) {
                    userInput = "quit";
                }
            }
        } catch (SQLException se) {
            System.out.println("Query Connection Failed");
        }
    }

    // FRS 4:
    private static void getReservationCancellation(Scanner scanner) throws SQLException {
        driverCheck();
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            String sqlStatement = "select * from anguy714.lab7_reservations\n" +
                    "where code = ?;";
            int cancellingReservationCode = 0;
            System.out.println("Please enter the reservation code for the reservation you want to cancel:");
            cancellingReservationCode = Integer.parseInt(scanner.nextLine());

            try (PreparedStatement stmt = conn.prepareStatement(sqlStatement);) {
                stmt.setInt(1, cancellingReservationCode);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    System.out.println("Reservation Code: " + rs.getInt("Code"));
                    System.out.println("Room Code: " + rs.getString("Room"));
                    System.out.println("Check In: " + rs.getDate("CheckIn"));
                    System.out.println("Check Out: " + rs.getDate("CheckOut"));
                    System.out.println("Rate: " + rs.getFloat("Rate"));
                    System.out.println("Last Name: " + rs.getString("LastName"));
                    System.out.println("First Name: " + rs.getString("FirstName"));
                    System.out.println("Adults: " + rs.getInt("Adults"));
                    System.out.println("Kids: " + rs.getInt("Kids"));
                    System.out.println("Enter [Y] to confirm cancellation or [N] to go back:");
                    String confirmation = scanner.nextLine();
                    if (confirmation.equalsIgnoreCase("Y")) {
                        sqlStatement = "delete from anguy714.lab7_reservations\n" +
                                "where code = ?";
                        try (PreparedStatement stmt2 = conn.prepareStatement(sqlStatement);) {
                            stmt2.setInt(1, cancellingReservationCode);
                            int rs2 = stmt2.executeUpdate();
                            if (rs2 == 1) {
                                System.out.println("Reservation Cancelled Successfully!");
                            }
                        }
                    } else {
                        System.out.println("Exiting cancellation process...");
                    }
                }
            }
        }
    }

    // FRS 5:
    private static void getReservationInformation(Scanner scanner) throws SQLException {
        driverCheck();
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            String FirstName = "";
            String LastName = "";
            String DateStart = "";
            String DateEnd = "";
            String RoomCode = "";
            String ReservationCode = "";

            System.out.println(
                    "Please provide the following information to search for a reservation (leave blank for ANY):");
            System.out.println("First Name: ");
            FirstName = scanner.nextLine();
            System.out.println("Last Name: ");
            LastName = scanner.nextLine();
            System.out.println("Range of dates the CheckIn and CheckOut date is in [yyyy-mm-dd yyyy-mm-dd]:");
            String DateInput = scanner.nextLine();
            if (DateInput.length() > 0) {
                String[] DateInputArray = DateInput.split(" ");
                DateStart = DateInputArray[0];
                DateEnd = DateInputArray[1];
            }
            System.out.println("Room Code: ");
            RoomCode = scanner.nextLine();
            System.out.println("Reservation Code: ");
            ReservationCode = scanner.nextLine();
            String sqlStatement = "select * from anguy714.lab7_reservations\n" +
                    "join anguy714.lab7_rooms on anguy714.lab7_reservations.Room = anguy714.lab7_rooms.RoomCode\n" +
                    "where 1=1";

            String[] wildCharacters = { "%", "_" };
            if (FirstName.length() > 0) {
                if (stringContainsFromArr(FirstName, wildCharacters)) {
                    sqlStatement += " and FirstName like ?";
                } else {
                    sqlStatement += " and FirstName = ?";
                }
            }
            if (LastName.length() > 0) {
                if (stringContainsFromArr(LastName, wildCharacters)) {
                    sqlStatement += " and LastName like ?";
                } else {
                    sqlStatement += " and LastName = ?";
                }
            }
            if (DateStart.length() > 0 && DateEnd.length() > 0) {
                sqlStatement += " and CheckIn >= ? and CheckOut <= ?";
            }
            if (RoomCode.length() > 0) {
                if (stringContainsFromArr(RoomCode, wildCharacters)) {
                    sqlStatement += " and Room like ?";
                } else {
                    sqlStatement += " and Room = ?";
                }
            }
            if (ReservationCode.length() > 0) {
                sqlStatement += " and Code = ?";
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlStatement);) {
                int currentSet = 1;
                if (FirstName.length() > 0) {
                    stmt.setString(currentSet, FirstName);
                    currentSet++;
                }
                if (LastName.length() > 0) {
                    stmt.setString(currentSet, LastName);
                    currentSet++;
                }
                if (DateStart.length() > 0 && DateEnd.length() > 0) {
                    stmt.setString(currentSet, DateStart);
                    currentSet++;
                    stmt.setString(currentSet, DateEnd);
                    currentSet++;
                }
                if (RoomCode.length() > 0) {
                    stmt.setString(currentSet, RoomCode);
                    currentSet++;
                }
                if (ReservationCode.length() > 0) {
                    stmt.setInt(currentSet, Integer.parseInt(ReservationCode));
                    currentSet++;
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    System.out.println("Reservation Code: " + rs.getInt("Code"));
                    System.out.println("Room: " + rs.getString("Room"));
                    System.out.println("Check In: " + rs.getDate("CheckIn"));
                    System.out.println("Check Out: " + rs.getDate("CheckOut"));
                    System.out.println("Rate: " + rs.getFloat("Rate"));
                    System.out.println("Last Name: " + rs.getString("LastName"));
                    System.out.println("First Name: " + rs.getString("FirstName"));
                    System.out.println("Adults: " + rs.getInt("Adults"));
                    System.out.println("Kids: " + rs.getInt("Kids"));
                    System.out.println("Room Name: " + rs.getString("RoomName"));
                    System.out.println();
                }
            }
        }

    }

    private static boolean stringContainsFromArr(String s, String[] items) {
        return Arrays.stream(items).anyMatch(s::contains);
    }

    // FRS 6:
    private static void getRevenue() throws SQLException {
        driverCheck();

        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            String sqlStatement = "with s as(\n" +
                    "SELECT 0 s\n" +
                    "   UNION ALL SELECT   1 UNION ALL SELECT   2 UNION ALL SELECT   3\n" +
                    "   UNION ALL SELECT   4 UNION ALL SELECT   5 UNION ALL SELECT   6\n" +
                    "   UNION ALL SELECT   7 UNION ALL SELECT   8 UNION ALL SELECT   9\n" +
                    "), t  as(\n" +
                    "    SELECT 0 t\n" +
                    "    UNION ALL SELECT  10 UNION ALL SELECT  20 UNION ALL SELECT  30\n" +
                    "    UNION ALL SELECT  40 UNION ALL SELECT  50 UNION ALL SELECT  60\n" +
                    "    UNION ALL SELECT  70 UNION ALL SELECT  80 UNION ALL SELECT  90\n" +
                    "), h as(\n" +
                    "    SELECT 0 h\n" +
                    "    UNION ALL SELECT  100 UNION ALL SELECT  200 UNION ALL SELECT  300\n" +
                    "    UNION ALL SELECT  400 UNION ALL SELECT  500 UNION ALL SELECT  600\n" +
                    "    UNION ALL SELECT  700 UNION ALL SELECT  800 UNION ALL SELECT  900\n" +
                    "), A as(\n" +
                    "    SELECT s + t + h as n \n" +
                    "    FROM s JOIN t JOIN h\n" +
                    "    ORDER BY n DESC\n" +
                    "), B as(\n" +
                    "    SELECT ('2022-12-31' - INTERVAL A.n DAY) AS date\n" +
                    "    FROM A \n" +
                    "    WHERE A.n BETWEEN 0 and 364\n" +
                    "), C as(\n" +
                    "    select rv.room as room, monthname(date) as month, sum(rate) as rev\n" +
                    "    from anguy714.lab7_reservations as rv\n" +
                    "        join B on B.date <= rv.checkout \n" +
                    "    where B.date >= rv.checkin\n" +
                    "    group by monthname(date), rv.room\n" +
                    "), D as(\n" +
                    "    select C.room, sum(rev) as TotalRevenue\n" +
                    "    from C\n" +
                    "    group by C.room\n" +
                    "), E as(\n" +
                    "    select C.room,\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='January' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'January',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='February' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'February',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='March' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'March',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='April' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'April',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='May' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'May',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='June' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'June',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='July' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'July',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='August' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'August',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='September' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'September',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='October' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'October',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='November' THEN C.rev\n" +
                    "                ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'November',\n" +
                    "    SUM(\n" +
                    "            CASE\n" +
                    "                WHEN C.month='December' THEN C.rev\n" +
                    "               ELSE 0\n" +
                    "            END\n" +
                    "        ) AS 'December'\n" +
                    "    from C\n" +
                    "    group by C.room\n" +
                    ")\n" +
                    "select D.Room, January, February, March, April, May, June, July, August, September, October, November, December, TotalRevenue\n"
                    +
                    "from D join  E on D.room = E.room;";
            try (Statement stmt = conn.createStatement();) {
                ResultSet rs = stmt.executeQuery(sqlStatement);
                // Print first row
                System.out.printf("%-5s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %n", "Room",
                        "January", "February", "March", "April", "May", "June", "July", "August", "September",
                        "October", "November", "December", "TotalRevenue");
                while (rs.next()) {
                    String Room = rs.getString("Room");
                    String Jan = rs.getString("January");
                    String Feb = rs.getString("February");
                    String Mar = rs.getString("March");
                    String Apr = rs.getString("April");
                    String May = rs.getString("May");
                    String Jun = rs.getString("June");
                    String Jul = rs.getString("July");
                    String Aug = rs.getString("August");
                    String Sep = rs.getString("September");
                    String Oct = rs.getString("October");
                    String Nov = rs.getString("November");
                    String Dec = rs.getString("December");
                    String TotalRev = rs.getString("TotalRevenue");
                    System.out.printf("%-5s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %-9s %n", Room,
                            Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, TotalRev);
                }
            }

        }
    }

    private static void driverCheck() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded");
        } catch (ClassNotFoundException ex) {
            System.err.println("Unable to load JDBC Driver");
            System.exit(-1);
        }
    }
}