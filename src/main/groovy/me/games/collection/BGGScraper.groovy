package me.games.collection

import groovy.xml.XmlSlurper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.io.IOException
import java.net.URL

@Component('bggScraper')
class BGGScraper {

    @Value('${bgg.api.baseurl}')
    private String searchBase

    private static final int MAX_RETRIES = 5
    private static final long INITIAL_BACKOFF_MS = 1000L // 1 second for initial backoff

    // Changed return type conceptually - now returns List<Map>
    def fetchCollection(String username) {
        String searchQuery = 'collection'
        String searchParameter = 'username'

        String queueMessage = 'Please try again later for access'
        String rawXmlContent = queueMessage // Initialize with queueMessage to enter loop
        int retryCount = 0

        // This loop fetches the raw XML content
        while (rawXmlContent.contains(queueMessage) && retryCount < MAX_RETRIES) {
            try {
                URL url = "$searchBase/$searchQuery?$searchParameter=$username".toURL()
                HttpURLConnection conn = (HttpURLConnection) url.openConnection()
                conn.setRequestProperty("User-Agent", "GroovyBGGScraper/1.0")
                conn.setConnectTimeout(15000)
                conn.setReadTimeout(30000)
                conn.setInstanceFollowRedirects(true)

                rawXmlContent = fetchFromUrl(conn) // Gets raw XML string

                if (rawXmlContent.contains(queueMessage)) {
                    retryCount++
                    long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                    println "BGG API queue message received for '$username'. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms before next attempt..."
                    sleep(backoffTime)
                }
            } catch (SocketTimeoutException ste) {
                retryCount++
                long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                println "Connection timed out while fetching collection for '$username': ${ste.getMessage()}. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms..."
                sleep(backoffTime)
                if (retryCount >= MAX_RETRIES) {
                    println "Max retries reached for '$username' due to SocketTimeoutException. Giving up."
                    // Instead of throwing here, let it fall through to XML parsing, which will fail if content is bad
                    // or return empty list if content is still queue message.
                    rawXmlContent = "Error: Max retries reached - SocketTimeoutException" // Ensure it's not the queue message
                    break // Exit retry loop
                }
            } catch (IOException ioe) {
                retryCount++
                long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                println "An IO error occurred while fetching collection for '$username': ${ioe.getMessage()}. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms..."
                sleep(backoffTime)
                if (retryCount >= MAX_RETRIES) {
                    println "Max retries reached for '$username' due to IOException. Giving up."
                    rawXmlContent = "Error: Max retries reached - IOException"
                    break // Exit retry loop
                }
            } catch (Exception e) {
                retryCount++
                long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                println "An unexpected error occurred while fetching collection for '$username': ${e.class.name} - ${e.getMessage()}. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms..."
                sleep(backoffTime)
                if (retryCount >= MAX_RETRIES) {
                    println "Max retries reached for '$username' due to an unexpected error. Giving up."
                    rawXmlContent = "Error: Max retries reached - Exception"
                    break // Exit retry loop
                }
            }
        }

        if (rawXmlContent.contains(queueMessage) && retryCount >= MAX_RETRIES) {
            // This case means persistent queue messages. Return empty list or throw specific error.
            println "Failed to fetch collection for '$username' after $MAX_RETRIES attempts due to persistent API queue messages."
            return [] // Return empty list for this case
        }

        if (rawXmlContent.startsWith("Error:")) {
             println "Failed to fetch collection for '$username'. $rawXmlContent"
             return [] // Return empty list for error cases after retries
        }

        if (rawXmlContent == queueMessage) { // Should ideally not happen if MAX_RETRIES > 0 due to loop logic
             println "Failed to fetch collection for '$username'. Initial API queue message was not cleared (MAX_RETRIES might be 0 or too low)."
             return []
        }

        // Now, parse the raw XML content and transform it to List<Map>
        try {
            def xml = new XmlSlurper().parseText(rawXmlContent)
            def gamesList = xml.children()
                    .findAll {
                        // Use .text() for attributes to get their string value before comparison
                        it.status.@own.text() == "1" && \
                        it.@objecttype.text() == 'thing' && \
                        it.@subtype.text() == 'boardgame'
                    }
                    .collect {
                        [
                            name: it.name.text(), // .text() on elements is usually fine
                            imageUrl: it.thumbnail.text(),
                            id: it.@objectid.text() // .text() on attribute access
                        ]
                    }
            return gamesList
        } catch (Exception parsingException) {
            println "Error parsing XML content for $username: ${parsingException.getMessage()}"
            // Log the problematic XML content for debugging if possible and not too large
            // println "Problematic XML: $rawXmlContent"
            return [] // Return empty list if parsing fails
        }
    }

    protected String fetchFromUrl(HttpURLConnection conn) throws IOException {
        int responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_ACCEPTED) { // BGG uses 202 for queue responses
            // For 202, BGG sends the "queued" message in the body.
            // It might be in errorStream or inputStream depending on server/client behavior.
            // Safest to check errorStream first if it's considered an "error" for typical HTTP.
            // However, BGG's 202 is more of a "please wait", so body is likely in inputStream.
            return conn.inputStream.text
        } else if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) { // Check for 4xx/5xx errors
            throw new IOException("Server returned HTTP error code $responseCode: ${conn.responseMessage}")
        }

        return conn.inputStream.text
    }
}
