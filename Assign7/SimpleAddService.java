import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets; // Import for Charset
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class SimpleAddService {

    public static void main(String[] args) throws IOException {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Handler for the calculation endpoint (returns JSON)
        server.createContext("/add", new AddHandler());

        // Handler for the HTML page (serves HTML)
        server.createContext("/", new CalculatorHtmlHandler());

        // Use a fixed thread pool to handle requests concurrently
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Addition Web Service started on port " + port);
        System.out.println("Access the calculator UI via: http://localhost:" + port + "/");
        System.out.println("API endpoint remains: http://localhost:" + port + "/add?num1=<value>&num2=<value>");
    }

    // Handler to serve the HTML page
    static class CalculatorHtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Only handle GET requests for the HTML page
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            // HTML content as a String
            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <title>Simple Adder</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h1>Simple Addition Web Service</h1>\n" +
                    "    <form id=\"addForm\">\n" +
                    "        <div>\n" +
                    "            <label for=\"num1\">Number 1:</label>\n" +
                    "            <input type=\"number\" id=\"num1\" name=\"num1\" step=\"any\">\n" +
                    "        </div>\n" +
                    "        <div>\n" +
                    "            <label for=\"num2\">Number 2:</label>\n" +
                    "            <input type=\"number\" id=\"num2\" name=\"num2\" step=\"any\">\n" +
                    "        </div>\n" +
                    "        <div>\n" +
                    "            <button type=\"button\" id=\"submitBtn\">Calculate Sum</button>\n" +
                    "        </div>\n" +
                    "    </form>\n" +
                    "\n" +
                    "    <div id=\"result\"></div>\n" +
                    "\n" +
                    "    <script>\n" +
                    "        document.getElementById('submitBtn').addEventListener('click', function() {\n" +
                    "            const num1 = document.getElementById('num1').value;\n" +
                    "            const num2 = document.getElementById('num2').value;\n" +
                    "            const resultDiv = document.getElementById('result');\n" +
                    "\n" +
                    "            if (num1 === '' || num2 === '') {\n" +
                    "                resultDiv.textContent = 'Error: Please enter both numbers.';\n" +
                    "                return;\n" +
                    "            }\n" +
                    "\n" +
                    "            const url = `/add?num1=${encodeURIComponent(num1)}&num2=${encodeURIComponent(num2)}`;\n" +
                    "            resultDiv.textContent = 'Calculating...';\n" +
                    "\n" +
                    "            fetch(url)\n" +
                    "                .then(response => response.json())\n" +
                    "                .then(data => {\n" +
                    "                    if (data.sum !== undefined) {\n" +
                    "                        resultDiv.textContent = `Result: ${data.num1} + ${data.num2} = ${data.sum}`;\n" +
                    "                    } else if (data.error) {\n" +
                    "                        resultDiv.textContent = `Error: ${data.error}`;\n" +
                    "                    } else {\n" +
                    "                        resultDiv.textContent = 'Error: Invalid response from server.';\n" +
                    "                    }\n" +
                    "                })\n" +
                    "                .catch(error => {\n" +
                    "                    resultDiv.textContent = `Error: ${error.message}`;\n" +
                    "                });\n" +
                    "        });\n" +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";

            sendResponse(exchange, 200, "text/html", htmlContent);
        }
    }


    // Handler for the calculation (remains mostly the same, returns JSON)
    static class AddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            int statusCode = 200; // OK
            String contentType = "application/json";

            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    statusCode = 405;
                    response = "{\"error\": \"Method Not Allowed\"}";
                } else {
                    URI requestedUri = exchange.getRequestURI();
                    Map<String, String> params = queryToMap(requestedUri.getQuery());

                    if (params != null && params.containsKey("num1") && params.containsKey("num2")) {
                        try {
                            // Allow empty strings temporarily, handle NumberFormatException
                            String num1Str = params.get("num1");
                            String num2Str = params.get("num2");
                            if (num1Str.isEmpty() || num2Str.isEmpty()) {
                                throw new NumberFormatException("Empty input provided");
                            }
                            double num1 = Double.parseDouble(num1Str);
                            double num2 = Double.parseDouble(num2Str);
                            double sum = num1 + num2;
                            response = "{\"num1\": " + num1 + ", \"num2\": " + num2 + ", \"sum\": " + sum + "}";
                            System.out.println("API Request: num1=" + num1 + ", num2=" + num2 + " -> Response: " + response);
                        } catch (NumberFormatException e) {
                            statusCode = 400;
                            response = "{\"error\": \"Invalid number format or empty input provided.\"}";
                            System.err.println("API Error: Invalid number format in request query: " + requestedUri.getQuery());
                        }
                    } else {
                        statusCode = 400;
                        response = "{\"error\": \"Missing 'num1' or 'num2' query parameters.\"}";
                        System.err.println("API Error: Missing parameters in request query: " + requestedUri.getQuery());
                    }
                }
            } catch (Exception e) {
                statusCode = 500;
                response = "{\"error\": \"Internal server error: " + e.getMessage() + "\"}";
                System.err.println("Internal Server Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                sendResponse(exchange, statusCode, contentType, response);
            }
        }

        // Helper method to parse query parameters (same as before)
        private Map<String, String> queryToMap(String query) {
             if (query == null || query.isEmpty()) {
                return null;
            }
            Map<String, String> result = new HashMap<>();
            // Use URLDecoder if needed, but simple split works for basic numbers
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                     // Basic URL decoding might be needed for complex values, but omitted for simplicity
                    result.put(entry[0], entry[1]);
                } else if (entry.length == 1 && !entry[0].isEmpty()) { // Handle case like "?param"
                    result.put(entry[0], "");
                }
            }
            return result;
        }
    }

     // Helper method to send HTTP response
    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        try {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
            throw e; // Re-throw exception after logging
        }
    }
}