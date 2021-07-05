package me.games.collection

import spock.lang.Specification

class BGGScraperTest extends Specification {
    def "test FetchCollection with mocked connection"() {
        given:
            BGGScraper bggScraper = Spy(BGGScraper) {
                fetchFromUrl(_) >> SpecHelper.getResourceContent('/mockResponse.xml')
            }

        when:
            def out = bggScraper.fetchCollection("123")

        then:
            out.item.size() == 4
    }
}
