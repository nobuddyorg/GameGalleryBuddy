package me.games.collection

import spock.lang.Specification
import spock.lang.Subject

class BGGScraperSpec extends Specification {

    @Subject
    BGGScraper scraper = Spy(new BGGScraper())

    def setup() {
        scraper.apiToken = "testtoken"
        scraper.sleepMs(_) >> { }
    }

    def "fetchCollection - successful fetch"() {

        given:
            def conn = Mock(HttpURLConnection)
            conn.getResponseCode() >> 200
            conn.getInputStream() >> new ByteArrayInputStream(
                    '<items><item objectid="1"><name>Game 1</name></item></items>'.bytes
            )
            scraper.openConnection(_) >> conn

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result.item.size() == 1
            result.item[0].name.text() == "Game 1"
    }

    def "fetchCollection - retries when in queue (202) then succeeds"() {

        given:
            def conn1 = Mock(HttpURLConnection)
            conn1.getResponseCode() >> 202

            def conn2 = Mock(HttpURLConnection)
            conn2.getResponseCode() >> 200
            conn2.getInputStream() >> new ByteArrayInputStream(
                    '<items><item objectid="2"><name>Game 2</name></item></items>'.bytes
            )

            scraper.openConnection(_) >>> [conn1, conn2]

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result.item.size() == 1
            result.item[0].name.text() == "Game 2"
            1 * scraper.sleepMs(5_000)
    }

    def "fetchCollection - handles exception and retries"() {

        given:
            def conn = Mock(HttpURLConnection)
            conn.getResponseCode() >> 200
            conn.getInputStream() >> new ByteArrayInputStream(
                    '<items><item objectid="3"><name>Game 3</name></item></items>'.bytes
            )

            scraper.openConnection(_) >> { throw new IOException("boom") } >> conn

        when:
            def result = scraper.fetchCollection("testuser")

        then:
            result.item.size() == 1
            result.item[0].name.text() == "Game 3"
            1 * scraper.sleepMs(5_000)
    }
}
