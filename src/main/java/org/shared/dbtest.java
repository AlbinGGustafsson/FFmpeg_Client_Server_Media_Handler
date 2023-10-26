package org.shared;

import java.text.DateFormat;
import java.time.LocalDateTime;

public class dbtest {

    public static void main(String[] args) throws Exception {

        SQliteManager sQliteManager = new SQliteManager("enckey");
//        sQliteManager.addJob("movie.mp4", "127.0.0.1", LocalDateTime.now().toString(), JobStatus.STARTED);
//        sQliteManager.addJob("movie2.mp4", "127.0.0.2", LocalDateTime.now().toString(), JobStatus.STARTED);
//        sQliteManager.addJob("movie3.mp4", "127.0.0.3", LocalDateTime.now().toString(), JobStatus.STARTED);
//        sQliteManager.addJob("movie.mp4", "127.0.0.1", LocalDateTime.now().toString(), JobStatus.FINISHED);

        String originalPassword = "superSecret123";
        //sQliteManager.addClientPassword("127.0.0.1", originalPassword);

        // Verify password for IP "127.0.0.1"
        String passwordToCheck = "yolo";  // This could come from user input in a real scenario
        boolean isPasswordCorrect = sQliteManager.verifyClientPassword("127.0.0.2", passwordToCheck);

        System.out.println("Password verification for 127.0.0.2: " + (isPasswordCorrect ? "SUCCESS" : "FAILED"));
//
//        System.out.println(sQliteManager.getJobs("127.0.0.1"));
//        System.out.println(sQliteManager.getJobs());

    }
}
