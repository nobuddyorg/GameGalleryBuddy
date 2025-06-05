package me.games.collection

import spock.lang.Specification
import java.net.SocketTimeoutException
import java.io.IOException
import java.net.HttpURLConnection // Required for metaClass signature
import groovy.xml.slurpersupport.GPathResult

class BGGScraperSpec extends Specification {

    BGGScraper scraper

    def setup() {
        scraper = new BGGScraper()
        // Manually set the value that @Value would inject
        scraper.searchBase = "https://api.geekdo.com/xmlapi2"
    }

    def "fetchCollection - successful fetch returns GPathResult"() {
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
            </items>'''
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn -> mockXml }

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result instanceof GPathResult
            result.name() == 'items'
            result.@totalitems.text() == "2"
            result.item.size() == 2
            result.item[0].name.text() == "Game 1"
            result.item[0].@objectid.text() == "1"
            result.item[0].status.@own.text() == "1"
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
            def result = scraper.fetchCollection("testuser")

        then:
            callCount == 3
            result instanceof GPathResult
            result.item.name.text() == "Game 10"
    }

    def "fetchCollection - returns error GPathResult if queue message persists after max retries"() {
        given:
            String queueMessage = "Please try again later for access"
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0

            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                return queueMessage
            }

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            callCount == maxRetries
            result instanceof GPathResult
            result.name() == 'items'
            result.error.text().contains("Persistent API queue messages")
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
                    responseAction()
                } else {
                    return responseAction
                }
            }

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            callCount == 3
            result instanceof GPathResult
            result.item.name.text() == "Game 30"
    }

    def "fetchCollection - returns error GPathResult if IOException persists after max retries"() {
        given:
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                throw new IOException("Persistent Network Error")
            }

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            callCount == maxRetries
            result instanceof GPathResult
            result.name() == 'items'
            result.error.text().contains("Max retries reached: IOException")
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
            def result = scraper.fetchCollection("testuser")

        then:
            callCount == 2
            result instanceof GPathResult
            result.item.name.text() == "Game 40"
    }

    def "fetchCollection - returns error GPathResult if SocketTimeoutException persists after max retries"() {
        given:
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                throw new SocketTimeoutException("Persistent Timeout Error")
            }

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            callCount == maxRetries
            result instanceof GPathResult
            result.name() == 'items'
            result.error.text().contains("Max retries reached: SocketTimeoutException")
    }

    def "fetchCollection - returns error GPathResult on parsing error"() {
        given:
            String malformedXml = "<items><item>malformed" // Missing closing tags
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn -> malformedXml }

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result instanceof GPathResult
            result.name() == 'items'
            result.error.text().contains("XML Parsing Error")
    }

    def cleanup() {
        // Instance-level metaClass changes are generally cleaned up by Spock when the instance goes out of scope.
    }
}
