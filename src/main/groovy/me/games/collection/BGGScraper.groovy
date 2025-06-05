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

    def fetchCollection(String username) {
        String searchQuery = 'collection'
        String searchParameter = 'username'

        String queueMessage = 'Please try again later for access'
        String rawXmlContent = queueMessage // Initialize rawXmlContent to enter retry loop
        int retryCount = 0

        while (rawXmlContent.contains(queueMessage) && retryCount < MAX_RETRIES) {
            try {
                URL url = "$searchBase/$searchQuery?$searchParameter=$username".toURL()
                HttpURLConnection conn = (HttpURLConnection) url.openConnection()
                conn.setRequestProperty("User-Agent", "GroovyBGGScraper/1.0")
                conn.setConnectTimeout(15000)
                conn.setReadTimeout(30000)
                conn.setInstanceFollowRedirects(true)

                rawXmlContent = fetchFromUrl(conn)

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
                    rawXmlContent = "Error: Max retries reached - SocketTimeoutException"
                    break
                }
            } catch (IOException ioe) {
                retryCount++
                long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                println "An IO error occurred while fetching collection for '$username': ${ioe.getMessage()}. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms..."
                sleep(backoffTime)
                if (retryCount >= MAX_RETRIES) {
                    println "Max retries reached for '$username' due to IOException. Giving up."
                    rawXmlContent = "Error: Max retries reached - IOException"
                    break
                }
            } catch (Exception e) {
                retryCount++
                long backoffTime = INITIAL_BACKOFF_MS * (2L ** (retryCount - 1))
                println "An unexpected error occurred while fetching collection for '$username': ${e.class.name} - ${e.getMessage()}. Retry ${retryCount}/${MAX_RETRIES}. Waiting ${backoffTime}ms..."
                sleep(backoffTime)
                if (retryCount >= MAX_RETRIES) {
                    println "Max retries reached for '$username' due to an unexpected error. Giving up."
                    rawXmlContent = "Error: Max retries reached - Exception"
                    break
                }
            }
        }

        if (rawXmlContent.contains(queueMessage) && retryCount >= MAX_RETRIES) {
            // Persistent queue messages after retries.
            println "Failed to fetch collection for '$username' after $MAX_RETRIES attempts due to persistent API queue messages."
            return []
        }

        if (rawXmlContent.startsWith("Error:")) {
             println "Failed to fetch collection for '$username'. $rawXmlContent"
             return []
        }

        if (rawXmlContent == queueMessage) {
             println "Failed to fetch collection for '$username'. Initial API queue message was not cleared (MAX_RETRIES might be 0 or too low)."
             return []
        }

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
                            name: it.name.text(),
                            imageUrl: it.thumbnail.text(),
                            id: it.@objectid.text()
                        ]
                    }
            return gamesList
        } catch (Exception parsingException) {
            println "Error parsing XML content for $username: ${parsingException.getMessage()}"
            return []
        }
    }

    protected String fetchFromUrl(HttpURLConnection conn) throws IOException {
        int responseCode = conn.responseCode
        // BGG uses 202 for queue message in body; read from inputStream.
        if (responseCode == HttpURLConnection.HTTP_ACCEPTED) {
            return conn.inputStream.text
        } else if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) { // Check for 4xx/5xx errors
            throw new IOException("Server returned HTTP error code $responseCode: ${conn.responseMessage}")
        }

        return conn.inputStream.text
    }
}
