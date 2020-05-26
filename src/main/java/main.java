import com.opencsv.CSVWriter;
import kong.unirest.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONArray;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;



public class main {

    static String dbName = "ZEtest";
    static String className = "org.h2.Driver";
    static String url = "jdbc:h2:~/" + dbName;
    public static Connection conn; // Thse are the dtabase things I use for H2


    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
        sdf.setLenient(false);                                             //Making the inputs robust so that it checks to make sure the two strings input are correct.

        main thing = new main();

        if (args.length == 2) {

            try {
                sdf.parse(args[0]);
                try {
                    sdf.parse(args[1]);

                    thing.toRun(args[0], args[1]);

                } catch (ParseException e) {
                    System.out.print("The date format is incorrect. Ensure it follows YYYY-MM-DD and try again");
                }
            } catch (ParseException e) {
                System.out.print("The date format is incorrect. Ensure it follows YYYY-MM-DD and try again");
            }
        }

        if (args.length == 0) {

            LocalDate end =LocalDate.now(); // Getting todays Date


            LocalDate start = end.minusDays(7);  // Getting the date one week earlier than today



            String endDate = end.toString();
            String startDate = start.toString();

            thing.toRun(startDate, endDate); // Beginning the call with todays date and the Date one week prior



        }
    }


        public void toRun (String date1, String date2){


            // Establishing the database so I can just loop the the HASHMAP values and sue the predefined DB
            Statement stmt = null;
            conn = null;
            if (conn == null) {
                try {
                    Class.forName(className);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    conn = DriverManager.getConnection(url, "sa", "sa");
                } catch (SQLException e) {
                    System.out.print("Error in Database Connection");
                }

            }
            try {
                stmt = conn.createStatement();

            } catch (SQLException e) {
                System.out.print("Error with creating statement");
            }

            try {
                String sqldrop = "DROP TABLE IF EXISTS COVID"; // This makes sure a table has been wiped when re runnig the program.
                stmt.executeUpdate(sqldrop);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String sql = "CREATE TABLE COVID " +

                    " (Date VARCHAR(255), " +
                    " Province VARCHAR(255), " +
                    " Confirmed INTEGER, " +
                    " Deaths INTEGER, " +
                    " Recovered INTEGER," +
                    " Active INTEGER) ";

            try {
                stmt.executeUpdate(sql);

            } catch (SQLException e) {
                e.printStackTrace();
            }


            List<ImmutablePair<String, String>> myPairKeys; // This is a list of keys I will use to write to the DB

            Map<myStrucutre, Integer> recoveredMap = new HashMap();
            Map<myStrucutre, Integer> deathsMap = new HashMap();
            Map<myStrucutre, Integer> confirmedMap = new HashMap();


            HttpResponse<String> responseReceovered = Unirest.get("https://api.covid19api.com/country/canada/status/recovered/live?from=" + date1 + "T00:00:00Z&to=" + date2 + "T00:00:00Z").asString();
            HttpResponse<String> responseConfirmed = Unirest.get("https://api.covid19api.com/country/canada/status/confirmed/live?from=" + date1 + "T00:00:00Z&to=" + date2 + "T00:00:00Z").asString();
            HttpResponse<String> responseDeaths = Unirest.get("https://api.covid19api.com/country/canada/status/deaths/live?from=" + date1 + "T00:00:00Z&to=" + date2 + "T00:00:00Z").asString();


            JSONArray arrayRecovered = new JSONArray(responseReceovered.getBody()); // This creates a json array from the get requests with recovered keywords for Recovered
            JSONArray arrayDeaths = new JSONArray(responseDeaths.getBody()); // This creates a json array from the get requests with recovered keywords for Deaths
            JSONArray arrayConfirmed = new JSONArray(responseConfirmed.getBody()); // This creates a json array from the get requests with recovered keywords for Confirmed
           // sql = "INSERT INTO COVID (Date,Province, Confirmed, deaths,Recovered, Active) VALUES(?,?,?,?,?,?)"; Unneescary  kept as a template example


            // Beginning of my second version. Will likely use this. It is more robus than the above. As it does not require the assumption that data is in the same ordder
            // However, that is a reasonable assumptions as I read the example respones to ensure that was correct. But robust is better.
            for (int i = 0; i < arrayConfirmed.length() - 1; i++) {

                String provinceRecovered = arrayRecovered.getJSONObject(i).getString("Province");
                String dateRecovered = arrayRecovered.getJSONObject(i).getString("Date");//

                String provinceConfirmed = arrayConfirmed.getJSONObject(i).getString("Province");
                String dateConfirmed = arrayConfirmed.getJSONObject(i).getString("Date");//

                String provinceDeaths = arrayRecovered.getJSONObject(i).getString("Province");
                String dateDeaths = arrayRecovered.getJSONObject(i).getString("Date");//
                // of case should be on

                myStrucutre recoveredKey = new myStrucutre(dateRecovered, provinceRecovered);

                recoveredMap.put(recoveredKey, arrayRecovered.getJSONObject(i).getInt("Cases"));

                myStrucutre confirmedKey = new myStrucutre(dateConfirmed, provinceConfirmed);

                confirmedMap.put(confirmedKey, arrayConfirmed.getJSONObject(i).getInt("Cases"));


                myStrucutre deathsKey = new myStrucutre(dateDeaths, provinceDeaths);
                deathsMap.put(deathsKey, arrayDeaths.getJSONObject(i).getInt("Cases"));
            }


            // Looping over the key to add it into the table
            for (myStrucutre struct : deathsMap.keySet()) {
                String dateForTable = struct.getDate();
                String provinceForTable = struct.getProvince();
                String sql2 = "INSERT INTO COVID (Date,Province) VALUES(?,?)";

                try {
                    PreparedStatement preparedStatement = conn.prepareStatement(sql2);

                    preparedStatement.setString(1, dateForTable);
                    preparedStatement.setString(2, provinceForTable);
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }


            }
            sql = "UPDATE COVID SET Confirmed =?, Deaths=?, Recovered = ?, Active = ? WHERE Date = ? AND Province = ?";
            //Looping through to put the associated confirmed, recovered, or deaths value associated with the key. I know keys are the same since dates are the same.
            for (myStrucutre thingy : deathsMap.keySet()
                    ) {
                String dateForUpdate = thingy.getDate();
                String provinceForUpdate = thingy.getProvince();

                Integer confirmed = confirmedMap.get(thingy);
                Integer deaths = deathsMap.get(thingy);
                Integer recovered = recoveredMap.get(thingy);
                Integer active = confirmed - deaths - recovered; // Im not sure if this is the correct calculation, but I believe it should be.

                try {

                    PreparedStatement preparedStatement2 = conn.prepareStatement(sql);
                    preparedStatement2.setInt(1, confirmed);
                    preparedStatement2.setInt(2, deaths);
                    preparedStatement2.setInt(3, recovered);
                    preparedStatement2.setInt(4, active);
                    preparedStatement2.setString(5, dateForUpdate);
                    preparedStatement2.setString(6, provinceForUpdate);
                    preparedStatement2.executeUpdate();


                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }



            try {
                sql = "SELECT * FROM COVID  ORDER BY Date, Province";
                PreparedStatement preparedStatement3 = conn.prepareStatement(sql);
                ResultSet resultSet = preparedStatement3.executeQuery();


                String fileName = "Covid.csv";
                try {
                    CSVWriter writer = new CSVWriter(new FileWriter(fileName), '\t');
                    writer.writeAll(resultSet, true);
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (SQLException e) {
                e.printStackTrace();

            }
        }
    }




















