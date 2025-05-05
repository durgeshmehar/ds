import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder; // For encoding query parameters
import java.util.Scanner;

public class SimpleAddClient {

    private static final String SERVICE_URL = "http://localhost:8000/add";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("Simple Addition Web Service Client");
            System.out.print("Enter the first number: ");
            String num1Str = scanner.nextLine();

            System.out.print("Enter the second number: ");
            String num2Str = scanner.nextLine();

            // Basic validation (ensure they are numbers before sending)
            try {
                Double.parseDouble(num1Str);
                Double.parseDouble(num2Str);
            } catch (NumberFormatException e) {
                System.err.println("Invalid input. Please enter numeric values.");
                return;
            }

            // Construct the URL with query parameters
            // Encode parameters to handle potential special characters (though less likely for numbers)
            String queryParams = String.format("?num1=%s&num2=%s",
                    URLEncoder.encode(num1Str, "UTF-8"),
                    URLEncoder.encode(num2Str, "UTF-8"));
            URL url = new URL(SERVICE_URL + queryParams);

            System.out.println("Connecting to: " + url);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds connection timeout
            connection.setReadTimeout(5000);    // 5 seconds read timeout

            int responseCode = connection.getResponseCode();

            System.out.println("Response Code: " + responseCode);

            StringBuilder response = new StringBuilder();
            // Use try-with-resources for BufferedReader
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    (responseCode >= 200 && responseCode <= 299) ? connection.getInputStream() : connection.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            System.out.println("Response Body: " + response.toString());

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Very basic JSON parsing to extract the sum
                String responseJson = response.toString();
                // Find the "sum" key and extract the value after the colon, trimming whitespace/braces
                int sumKeyIndex = responseJson.indexOf("\"sum\":");
                if (sumKeyIndex != -1) {
                    String sumPart = responseJson.substring(sumKeyIndex + "\"sum\":".length());
                    sumPart = sumPart.replaceAll("[^\\d.-]", ""); // Remove non-numeric characters except dot and minus
                    try {
                         double sum = Double.parseDouble(sumPart);
                         System.out.println("\nSuccessfully retrieved sum from service: " + sum);
                    } catch (NumberFormatException e) {
                         System.err.println("\nCould not parse 'sum' value from response: " + sumPart);
                    }

                } else {
                    System.err.println("\nCould not find 'sum' in the response JSON.");
                }
            } else {
                System.err.println("\nWeb service returned an error.");
            }

        } catch (IOException e) {
            System.err.println("An error occurred while contacting the web service: " + e.getMessage());
            // Check for common connection errors
            if (e instanceof java.net.ConnectException) {
                System.err.println("Hint: Is the SimpleAddService running?");
            }
        } finally {
            scanner.close(); // Close the scanner
        }
    }
}