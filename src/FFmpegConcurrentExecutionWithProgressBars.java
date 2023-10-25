import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FFmpegConcurrentExecutionWithProgressBars {
    public static void main(String[] args) {
        try {
            // Create two threads to execute the FFmpeg commands concurrently
            Thread thread1 = new Thread(() -> executeFFmpegCommand("test.mp4", "otest.mp4", "Video 1"));
            Thread thread2 = new Thread(() -> executeFFmpegCommand("test2.mp4", "otest2.mp4", "Video 2"));

            // Start both threads
            thread1.start();
            thread2.start();

            // Wait for both threads to finish
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void executeFFmpegCommand(String inputFileName, String outputFileName, String videoName) {
        try {
            // Command to extract the first 30 seconds of the input video
            String[] cmd = {
                    "ffmpeg",
                    "-i",
                    inputFileName,
                    "-c:v",
                    "libx264",
                    "-b:v",
                    "2000k",
                    "-c:a",
                    "aac",
                    "-b:a",
                    "128k",
                    "-t",
                    "30", // Duration in seconds
                    outputFileName
            };

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();

            // Create a thread to capture and print progress
            Thread progressThread = new Thread(() -> {
                try {
                    InputStream inputStream = process.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    // Variables for tracking progress
                    int durationInSeconds = 30;
                    int currentProgress = 0;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("time=")) {
                            String[] parts = line.split("time=");
                            if (parts.length > 1) {
                                String[] timeParts = parts[1].split("[:.]");
                                if (timeParts.length >= 4) {
                                    int hours = Integer.parseInt(timeParts[0]);
                                    int minutes = Integer.parseInt(timeParts[1]);
                                    int seconds = Integer.parseInt(timeParts[2]);
                                    int totalSeconds = hours * 3600 + minutes * 60 + seconds;
                                    currentProgress = (totalSeconds * 100) / durationInSeconds;
                                    System.out.print("\r" + videoName + " Progress: [" + progressBar(currentProgress) + "] " + currentProgress + "%");
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Start the progress thread
            progressThread.start();

            // Wait for the FFmpeg process to finish
            int exitCode = process.waitFor();
            System.out.println("\n" + videoName + " Command exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String progressBar(int progress) {
        StringBuilder progressBar = new StringBuilder("[");
        int length = 50; // Length of the progress bar

        int numBlocks = (int) (((double) progress / 100) * length);
        for (int i = 0; i < length; i++) {
            if (i < numBlocks) {
                progressBar.append("#");
            } else {
                progressBar.append(" ");
            }
        }

        progressBar.append("]");
        return progressBar.toString();
    }
}