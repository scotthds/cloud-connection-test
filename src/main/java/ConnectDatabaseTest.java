import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.Row;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConnectDatabaseTest {


    private final String keyspace =  System.getProperty("keyspace");
    private final String pathToCreds = System.getProperty("pathToCreds");
    private final String user = System.getProperty("user");
    private final String password = System.getProperty("password");
    private final boolean dbwriter = Boolean.parseBoolean(System.getProperty("dbwriter"));
    private static ByteBuffer buffer;
    private static String petComment;


    public static void main(String[] args) {

        try {
            ConnectDatabaseTest cdt = new ConnectDatabaseTest();
            System.out.println("user is " + cdt.user);
            // pet blob
            buffer = cdt.createBlobBuffer();

            // pet comment
            petComment = new String(Files.readAllBytes(Paths.get("pet_comment.txt")));

            // Create the DseSession object:
            try (DseSession session = DseSession.builder().withCloudSecureConnectBundle(cdt.pathToCreds)
                    .withAuthCredentials(cdt.user, cdt.password)
                    .withKeyspace(cdt.keyspace)
                    .build()) {


                if (cdt.dbwriter) {
                    System.out.println("Writer");
                    while (true) {
                        cdt.insertHeartRate(session);
                    }
                } else {
                    while (true) {
                        cdt.readHeartRate(session);
                    }
                }


            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }



    }

    public ByteBuffer createBlobBuffer() {
        // get pet blob 5M
        ByteBuffer buffer = ByteBuffer.allocate(5000000);
        while (buffer.hasRemaining())
            buffer.put((byte) 0xFF);

        buffer.flip();

        return buffer;
    }

    public void createTable(DseSession session)
    {
        try {

            // create the heartrate table
            session.execute("create table IF NOT EXISTS baselines.heartrate_v2 ( pet_chip_id uuid, time bigint, heart_rate int, comment text, pet_blob blob,  PRIMARY KEY (pet_chip_id, time))");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertHeartRate(DseSession session)
    {
        try {

            // get time
            long unixTime = System.currentTimeMillis();

            // prepare insert
            PreparedStatement prepared = session.prepare(
                    "insert into baselines.heartrate_v2 (pet_chip_id, time, heart_rate, comment, pet_blob) values (uuid(), ?, 98, ?, ?)");

            BoundStatement bound = prepared.bind(unixTime, petComment, buffer);
            session.execute(bound);

            System.out.println(unixTime);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readHeartRate(DseSession session)
    {

        try {
            // get time
            long unixTime = System.currentTimeMillis();

            // check results
            ResultSet rs = session.execute("select distinct pet_chip_id from heartrate_v2 limit 5");

            for(Row row : rs) {

                java.util.UUID pci = row.getUuid("pet_chip_id");
                System.out.println("Pet Chip ID "+pci);

                // prepare insert
                PreparedStatement prepared = session.prepare(
                        "insert into baselines.heartrate_v2 (pet_chip_id, time, heart_rate, comment, pet_blob) values (?, ?, 99, ?, ?)");

                BoundStatement bound = prepared.bind(pci,unixTime, petComment, buffer);
                session.execute(bound);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
