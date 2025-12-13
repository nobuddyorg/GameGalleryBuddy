package me.games.collection

import groovy.xml.XmlSlurper
import org.springframework.stereotype.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Component('bggScraper')
class BGGScraper {

    private static final Logger log = LoggerFactory.getLogger(BGGScraper)

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

        while (true) {
            try {
                HttpURLConnection conn = openConnection(urlString)
                conn.setRequestMethod('GET')
                conn.setRequestProperty('Authorization', "Bearer $apiToken")
                conn.instanceFollowRedirects = true

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
                break
            } catch (Exception e) {
                sleepMs(5_000)
            }
        }

        new XmlSlurper().parseText(content)
    }
}
