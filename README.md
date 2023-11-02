# 365lab7

**Team members:**<br />
Israel Banez <br />
Almas Perneshev <br />
Adrian Nguyen

_Note:_<br />
The database is under user anguy714.<br />
The database holds tables lab7_rooms and lab7_reservations.

**How to compile:**<br />
javac InnReservations.java<br />
java -cp mysql-connector-java-8.0.16.jar _[InnReservations | InnReservations.java]_

**Set Environment Variables (Windows):**<br />
setx HP_JDBC_URL "jdbc:mysql://db.labthreesixfive.com/anguy714?autoReconnect=true&useSSL=false"<br />
setx HP_JDBC_USER "anguy714"<br />
setx HP_JDBC_PW " "

**Set Environment Varables (Mac):**<br />
export HP_JDBC_URL="jdbc:mysql://db.labthreesixfive.com/anguy714?autoReconnect=true&useSSL=false" <br />
export HP_JDBC_USER=anguy714 <br />
export HP_JDBC_PW=

**Overall Code Status:**<br />
We have an implmentation for all 6 FRS methods. <br />
<br />
FR1 Notes -
We change the day range from 180 to 360, because the current date would not reach the dates of the data.<br />
FR2 Notes -<br />
The deficiencies of FRS 2 are that it requires users to input correct date format.<br />
It also lacks the 5 recommondations behavior when there is no available rooms. <br />
FR3 Notes - <br />
It lacks the ability to check for time conflictsc<br />
