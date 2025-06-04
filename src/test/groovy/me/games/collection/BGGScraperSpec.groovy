package me.games.collection

import spock.lang.Specification
import spock.lang.Subject
import java.net.SocketTimeoutException
import java.io.IOException

class BGGScraperSpec extends Specification {

    @Subject
    BGGScraper scraper // No need for Spy() if we are testing its public method and it calls its own protected method

    def setup() {
        // Initialize scraper and manually set the value that @Value would inject
        scraper = new BGGScraper()
        scraper.searchBase = "https://api.geekdo.com/xmlapi2" // Ensure this is set for tests
    }

    def "fetchCollection - successful fetch"() {
        given:
            String mockXml = '''<items totalitems="1"><item objectid="1"><name>Game 1</name></item></items>'''
            // Use a closure to define the mock behavior for fetchFromUrl
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
            callCount == 3 // Ensure it retried twice
            result.item.size() == 1
            result.item[0].name.text() == "Game 2"
    }

    def "fetchCollection - throws RuntimeException if queue message persists after max retries"() {
        given:
            String queueMessage = "Please try again later for access"
            int maxRetries = BGGScraper.MAX_RETRIES // Access static final field
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
            callCount == 3 // Ensure it retried twice
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
            callCount == 2 // Ensure it retried once
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
            // This simulates the behavior of fetchFromUrl when it encounters an HTTP error
            throw new IOException("Server returned HTTP error code 404: Not Found")
        }

        when:
        scraper.fetchCollection("nonexistentuser")

        then:
        // The exception should propagate after retries
        thrown(IOException)
    }

    // Cleanup metaclass modifications if any were made globally, though here it's instance-specific.
    // Spock typically handles cleanup of mocks and stubs defined with its own mechanisms.
    // For metaClass changes on the scraper instance, they are usually contained to the instance.
    // If we were modifying BGGScraper.metaClass (static), cleanup would be more critical.
    def cleanup() {
        // GroovySystem.metaClassRegistry.removeMetaClass(BGGScraper.class) // If we did global metaclass changes
    }
}
