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
    private static final long INITIAL_BACKOFF_MS = 1000L // 1 second

    def fetchCollection(String username) {
        String searchQuery = 'collection'
        String searchParameter = 'username'

        String queueMessage = 'Please try again later for access'
        String content = queueMessage
        int retryCount = 0

        while (content.contains(queueMessage) && retryCount < MAX_RETRIES) {
            try {
                URL url = "$searchBase/$searchQuery?$searchParameter=$username".toURL()
                HttpURLConnection conn = (HttpURLConnection) url.openConnection()
                conn.setRequestProperty("User-Agent", "GroovyBGGScraper/1.0") // Set a User-Agent
                conn.setConnectTimeout(15000) // 15 seconds connect timeout
                conn.setReadTimeout(30000) // 30 seconds read timeout
                conn.setInstanceFollowRedirects(true) // Already true by default for HttpURLConnection but explicit
                // HttpURLConnection.setFollowRedirects(true) // This is static and global, generally not recommended to change globally

                content = fetchFromUrl(conn)

                if (content.contains(queueMessage)) {
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
                    throw ste // Re-throw the exception or handle as appropriate
                }
            } catch (IOException ioe) {
                retryCount++
                long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                println "An IO error occurred while fetching collection for '$username': ${ioe.getMessage()}. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms..."
                sleep(backoffTime)
                if (retryCount >= MAX_RETRIES) {
                    println "Max retries reached for '$username' due to IOException. Giving up."
                    throw ioe // Re-throw the exception or handle as appropriate
                }
            } catch (Exception e) {
                retryCount++
                long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                // It's often better to catch more specific exceptions than a broad Exception
                println "An unexpected error occurred while fetching collection for '$username': ${e.class.name} - ${e.getMessage()}. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms..."
                sleep(backoffTime)
                if (retryCount >= MAX_RETRIES) {
                    println "Max retries reached for '$username' due to an unexpected error. Giving up."
                    throw e // Re-throw the exception or handle as appropriate
                }
            }
        }

        if (content.contains(queueMessage) && retryCount >= MAX_RETRIES) {
            throw new RuntimeException("Failed to fetch collection for '$username' after $MAX_RETRIES attempts due to persistent API queue messages.")
        }

        if (content == queueMessage) { // If content is still the initial queue message after loop (e.g. if MAX_RETRIES was 0)
             throw new RuntimeException("Failed to fetch collection for '$username'. Initial API queue message was not cleared.")
        }

        return new XmlSlurper().parseText(content)
    }

    protected String fetchFromUrl(HttpURLConnection conn) throws IOException {
        // Check for non-200 status codes if necessary, though BGG API might use 202 for queue
        int responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_ACCEPTED) { // 202
            // This is the BGG queue message, try to read from error stream or main stream
            // The existing logic handles this by checking content, so direct text reading is fine
            return conn.inputStream.text // Or errorStream if appropriate for 202
        } else if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) { // 400+
            throw new IOException("Server returned HTTP error code $responseCode: ${conn.responseMessage}")
        }

        // The BGG API sometimes redirects to a temporary URL which then contains the actual data.
        // The original code used getHeaderField("Location") which implies the primary response isn't the data.
        // However, if the API is truly redirecting, HttpURLConnection handles it automatically if instanceFollowRedirects is true.
        // The direct .text call on the URL was likely for the *initial* request, not after openConnection().
        // If conn.inputStream.text works after a redirect, that's simpler.
        // Let's assume the main content is in the input stream after redirects are handled.
        return conn.inputStream.text
    }
}
