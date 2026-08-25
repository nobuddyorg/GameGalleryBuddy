package me.games.collection

import groovy.xml.XmlSlurper
import org.springframework.stereotype.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Component('bggScraper')
class BGGScraper {

    private static final Logger log = LoggerFactory.getLogger(BGGScraper)

    private static final int CONNECT_TIMEOUT_MS = 10_000
    private static final int READ_TIMEOUT_MS = 10_000
    private static final int MAX_ATTEMPTS = 20

    String apiToken = System.getenv('BGG_API_TOKEN')

    protected HttpURLConnection openConnection(String urlString) {
        (HttpURLConnection) new URL(urlString).openConnection()
    }

    protected void sleepMs(long ms) {
        sleep(ms)
    }

    def fetchCollection(String username) {

        if (!apiToken) {
            log.error('BGG_API_TOKEN is not set. Cannot call BoardGameGeek XML API.')
            throw new IllegalStateException('BGG_API_TOKEN is not set')
        }

        String baseUrl = 'https://boardgamegeek.com/xmlapi2/collection'
        String urlString = "$baseUrl?username=$username&stats=1"

        String content
        Exception lastError

        for (int attempt = 1; attempt <= MAX_ATTEMPTS && !content; attempt++) {
            try {
                HttpURLConnection conn = openConnection(urlString)
                conn.setRequestMethod('GET')
                conn.setRequestProperty('Authorization', "Bearer $apiToken")
                conn.instanceFollowRedirects = true
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS

                int status = conn.responseCode

                if (status == 202) {
                    sleepMs(5_000)
                    continue
                }

                if (status != 200) {
                    String errorBody = conn.errorStream?.getText('UTF-8')
                    throw new RuntimeException("BGG returned $status: $errorBody")
                }

                content = conn.inputStream.getText('UTF-8')
            } catch (Exception e) {
                lastError = e
                sleepMs(5_000)
            }
        }

        if (!content) {
            throw new IllegalStateException("Failed to fetch BGG collection for '$username' after $MAX_ATTEMPTS attempts", lastError)
        }

        new XmlSlurper().parseText(content)
    }
}
