import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.Row;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;

public class ConnectDatabaseTest {

    private final String keyspace = System.getenv("DSE_KEYSPACE");
    private final String pathToCreds = System.getenv("DSE_CREDS_BUNDLE");
    private final String user = System.getenv("DSE_USER");
    private final String password = System.getenv("DSE_PASS");
    private final boolean dbWriter = Boolean.parseBoolean(System.getenv("DB_WRITER"));
    private final String sleepTimeEnv = System.getenv("SLEEP_TIME");

    private static String petComment;

    public static void main(String[] args) throws Exception {

        ConnectDatabaseTest cdt = new ConnectDatabaseTest();
        System.out.println("user is " + cdt.user);

        // pet comment
        petComment = new String(Files.readAllBytes(Paths.get("pet_comment.txt")));

        int sleepTime = 1000;
        if (cdt.sleepTimeEnv != null) {
            sleepTime = Integer.parseInt(cdt.sleepTimeEnv);
        }

        // Create the DseSession object:
        try (DseSession session = 
                DseSession.builder()
                          .withCloudSecureConnectBundle(cdt.pathToCreds)
                          .withAuthCredentials(cdt.user, cdt.password)
                          .withKeyspace(cdt.keyspace)
                          .build()) {

            cdt.createTable(session);

            if (cdt.dbWriter) {
                System.out.println("Write only");
            } else {
                System.out.println("Read + writes");
            }

            while (true) {
                if (cdt.dbWriter) {
                    cdt.insertHeartRate(session);
                } else {
                    cdt.readHeartRate(session);
                }
                Thread.sleep(sleepTime);
            }

        }

    }

    public ByteBuffer createBlobBuffer() {
        // get pet blob 1M
        byte[] bytes = new byte[1024 * 1024];
        new java.util.Random().nextBytes(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer;
    }

    public void createTable(DseSession session) {
        // create the heartrate table
        session.execute(
                "create table IF NOT EXISTS heartrate_v2 ( pet_chip_id uuid, time bigint, heart_rate int, comment text, pet_blob blob,  PRIMARY KEY (pet_chip_id, time))");

    }

    public void insertHeartRate(DseSession session) {
        // get time
        long unixTime = System.currentTimeMillis();

        // prepare insert
        PreparedStatement prepared = session.prepare(
                "insert into heartrate_v2 (pet_chip_id, time, heart_rate, comment, pet_blob) values (uuid(), ?, 98, ?, ?)");

        ByteBuffer buffer = createBlobBuffer();
        BoundStatement bound = prepared.bind(unixTime, petComment, buffer);
        CompletionStage<AsyncResultSet> cs = session.executeAsync(bound);
        cs.whenComplete((ars, th) -> {
            if (th != null) {
                System.err.println("async error for " + unixTime);
                th.printStackTrace();
            } else {
                System.out.println("ok " + unixTime);
            }
        });
    }

    public void readHeartRate(DseSession session) {

        // get time
        long unixTime = System.currentTimeMillis();

        // check results
        long token = new java.util.Random().nextLong();
        ResultSet rs = session.execute(
            "select distinct pet_chip_id from heartrate_v2 where token(pet_chip_id) > " + token + " limit 10");

        for (Row row : rs) {

            java.util.UUID pci = row.getUuid("pet_chip_id");
            System.out.println("Pet Chip ID " + pci);

            // prepare insert
            PreparedStatement prepared = session.prepare(
                    "insert into heartrate_v2 (pet_chip_id, time, heart_rate, comment, pet_blob) values (?, ?, 99, ?, ?)");

            ByteBuffer buffer = createBlobBuffer();
            BoundStatement bound = prepared.bind(pci, unixTime, petComment, buffer);
            CompletionStage<AsyncResultSet> cs = session.executeAsync(bound);
            cs.whenComplete((ars, th) -> {
                if (th != null) {
                    System.err.println("async error for " + pci);
                    th.printStackTrace();
                } else {
                    System.out.println("ok " + pci);
                }
            });
        }

    }

}
