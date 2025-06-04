package me.games.collection

import spock.lang.Specification
import spock.lang.Subject
import java.net.SocketTimeoutException
import java.io.IOException

class BGGScraperSpec extends Specification {

    @Subject
    BGGScraper scraper

    def setup() {
        scraper = new BGGScraper()
        scraper.searchBase = "https://api.geekdo.com/xmlapi2"
    }

    def "fetchCollection - successful fetch"() {
        given:
            String mockXml = '''<items totalitems="1"><item objectid="1"><name>Game 1</name></item></items>'''
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn -> mockXml }

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result instanceof groovy.xml.slurpersupport.GPathResult
            result.item.size() == 1
            result.item[0].name.text() == "Game 1"
    }

    def "fetchCollection - retries when in queue then succeeds"() {
        given:
            String queueMessage = "Please try again later for access"
            String mockXml = '''<items totalitems="1"><item objectid="2"><name>Game 2</name></item></items>'''
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
            result.item.size() == 1
            result.item[0].name.text() == "Game 2"
    }

    def "fetchCollection - throws RuntimeException if queue message persists after max retries"() {
        given:
            String queueMessage = "Please try again later for access"
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0

            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                return queueMessage
            }

        when:
            scraper.fetchCollection("testuser")

        then:
            thrown(RuntimeException)
            callCount == maxRetries
    }

    def "fetchCollection - handles IOException and retries then succeeds"() {
        given:
            String mockXml = '''<items totalitems="1"><item objectid="3"><name>Game 3</name></item></items>'''
            def responses = [{ throw new IOException("Network Error") }, { throw new IOException("Another Network Error") }, mockXml]
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
            result.item[0].name.text() == "Game 3"
    }

    def "fetchCollection - throws IOException if it persists after max retries"() {
        given:
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                throw new IOException("Persistent Network Error")
            }

        when:
            scraper.fetchCollection("testuser")

        then:
            thrown(IOException)
            callCount == maxRetries
    }

    def "fetchCollection - handles SocketTimeoutException and retries then succeeds"() {
        given:
            String mockXml = '''<items totalitems="1"><item objectid="4"><name>Game 4</name></item></items>'''
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
            result.item[0].name.text() == "Game 4"
    }

    def "fetchCollection - throws SocketTimeoutException if it persists after max retries"() {
        given:
            int maxRetries = BGGScraper.MAX_RETRIES
            int callCount = 0
            scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
                callCount++
                throw new SocketTimeoutException("Persistent Timeout Error")
            }

        when:
            scraper.fetchCollection("testuser")

        then:
            thrown(SocketTimeoutException)
            callCount == maxRetries
    }

    def "fetchCollection - throws IOException if fetchFromUrl indicates an HTTP error (e.g. 404)"() {
        given:
        scraper.metaClass.fetchFromUrl = { HttpURLConnection conn ->
            throw new IOException("Server returned HTTP error code 404: Not Found")
        }

        when:
        scraper.fetchCollection("nonexistentuser")

        then:
        thrown(IOException)
    }

    def cleanup() {
        // Intentionally empty, Spock handles most mock/stub cleanup.
        // MetaClass changes on instances are generally garbage collected with the instance.
    }
}
