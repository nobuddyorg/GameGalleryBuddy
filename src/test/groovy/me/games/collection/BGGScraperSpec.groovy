package me.games.collection

import spock.lang.Specification
import spock.lang.Subject

class BGGScraperSpec extends Specification {

    @Subject
    BGGScraper scraper = Spy(BGGScraper)

    def "fetchCollection - successful fetch"() {
        given:
            String mockXml = '''<items><item objectid="1"><name>Game 1</name></item></items>'''
            scraper.fetchFromUrl(_) >> mockXml

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result.item.size() == 1
            result.item[0].name.text() == "Game 1"
    }

    def "fetchCollection - retries when in queue"() {
        given:
            String queueMessage = "Please try again later for access"
            String mockXml = '''<items><item objectid="2"><name>Game 2</name></item></items>'''

            scraper.fetchFromUrl(_) >>> [queueMessage, mockXml]

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result.item.size() == 1
            result.item[0].name.text() == "Game 2"
    }

    def "fetchCollection - handles exception and retries"() {
        given:
            String mockXml = '''<items><item objectid="3"><name>Game 3</name></item></items>'''

            scraper.fetchFromUrl(_) >> { throw new IOException("boom") } >> mockXml

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result.item[0].name.text() == "Game 3"
    }
}
