package me.games.collection

import spock.lang.Specification
import java.net.SocketTimeoutException
import java.io.IOException
import java.net.HttpURLConnection // Required for metaClass signature

class BGGScraperSpec extends Specification {

    BGGScraper scraper

    def setup() {
        scraper = new BGGScraper()
        // Manually set the value that @Value would inject, as it's used by the real fetchCollection
        scraper.searchBase = "https://api.geekdo.com/xmlapi2"
    }

    def "fetchCollection - successful fetch returns list of game maps"() {
        given:
            String mockXml = '''
            <items totalitems="2" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="1" subtype="boardgame">
                    <name type="primary">Game 1</name>
                    <thumbnail>g1.jpg</thumbnail>
                    <status own="1"/>
                </item>
                <item objecttype="thing" objectid="2" subtype="boardgame">
                    <name type="primary">Game 2 (Not Owned)</name>
                    <thumbnail>g2.jpg</thumbnail>
                    <status own="0"/>
                </item>
                <item objecttype="thing" objectid="3" subtype="boardgameexpansion">
                    <name type="primary">Game 3 (Expansion)</name>
                    <thumbnail>g3.jpg</thumbnail>
                    <status own="1"/>
                </item>
                <item objecttype="thing" objectid="4" subtype="boardgame">
                    <name type="primary">Game 4</name>
                    <thumbnail>g4.jpg</thumbnail>
                    <status own="1"/>
                </item>
            </items>'''
            // Stub the protected fetchFromUrl method
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn -> mockXml }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            result instanceof List
            result.size() == 2 // Game 2 (not owned) and Game 3 (expansion) should be filtered out
            result[0].name == "Game 1"
            result[0].id == "1"
            result[0].imageUrl == "g1.jpg"
            result[1].name == "Game 4"
            result[1].id == "4"
            result[1].imageUrl == "g4.jpg"
    }

    def "fetchCollection - retries when in queue then succeeds"() {
        given:
            String queueMessage = "Please try again later for access"
            String mockXml = '''
            <items><item objecttype="thing" objectid="10" subtype="boardgame">
                <name>Game 10</name><thumbnail>g10.jpg</thumbnail><status own="1"/>
            </item></items>'''
            def responses = [queueMessage, queueMessage, mockXml]
            int callCount = 0

            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                def response = responses[callCount]
                callCount++
                return response
            }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            callCount == 3
            result.size() == 1
            result[0].name == "Game 10"
    }

    def "fetchCollection - returns empty list if queue message persists after max retries"() {
        given:
            String queueMessage = "Please try again later for access"
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0

            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                return queueMessage
            }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            callCount == maxRetries
            result instanceof List
            result.isEmpty()
    }

    def "fetchCollection - handles IOException and retries then succeeds"() {
        given:
            String mockXml = '''
            <items><item objecttype="thing" objectid="30" subtype="boardgame">
                <name>Game 30</name><thumbnail>g30.jpg</thumbnail><status own="1"/>
            </item></items>'''
            // Simulate two IOExceptions then a successful XML response
            def responses = [
                { throw new IOException("Network Error") },
                { throw new IOException("Another Network Error") },
                mockXml
            ]
            int callCount = 0

            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                def responseAction = responses[callCount]
                callCount++
                if (responseAction instanceof Closure) {
                    responseAction() // This will throw the exception
                } else {
                    return responseAction // This will return the mockXml
                }
            }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            callCount == 3
            result.size() == 1
            result[0].name == "Game 30"
    }

    def "fetchCollection - returns empty list if IOException persists after max retries"() {
        given:
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                throw new IOException("Persistent Network Error")
            }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            callCount == maxRetries
            result instanceof List
            result.isEmpty()
    }

    def "fetchCollection - handles SocketTimeoutException and retries then succeeds"() {
        given:
            String mockXml = '''
            <items><item objecttype="thing" objectid="40" subtype="boardgame">
                <name>Game 40</name><thumbnail>g40.jpg</thumbnail><status own="1"/>
            </item></items>'''
            def responses = [{ throw new SocketTimeoutException("Timeout!") }, mockXml]
            int callCount = 0

            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                def responseAction = responses[callCount]
                callCount++
                if (responseAction instanceof Closure) {
                    responseAction()
                } else {
                    return responseAction
                }
            }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            callCount == 2
            result.size() == 1
            result[0].name == "Game 40"
    }

    def "fetchCollection - returns empty list if SocketTimeoutException persists after max retries"() {
        given:
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                throw new SocketTimeoutException("Persistent Timeout Error")
            }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            callCount == maxRetries
            result instanceof List
            result.isEmpty()
    }

    def "fetchCollection - returns empty list on parsing error"() {
        given:
            String malformedXml = "<items><item>malformed" // Missing closing tags
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn -> malformedXml }

        when:
            List<Map<String, String>> result = scraper.fetchCollection("testuser")

        then:
            result instanceof List
            result.isEmpty()
            // Optionally, verify a log message if your actual code logs parsing exceptions
            // This requires more advanced Spock features or checking console output if applicable
    }


    def cleanup() {
        // Reset metaClass modifications if necessary, though Spock usually handles this for instance-level metaClass changes.
        // GroovySystem.metaClassRegistry.removeMetaClass(BGGScraper.class) // Example if global changes were made
    }
}
