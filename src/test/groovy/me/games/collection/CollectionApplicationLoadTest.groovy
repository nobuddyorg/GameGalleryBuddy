package me.games.collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = CollectionApplication.class)
class CollectionApplicationLoadTest extends Specification {

    @Autowired(required = false)
    private BGGScraper bggScraper

    def "when context is loaded then all expected beans are created"() {
        expect: "the scraper is created"
        bggScraper
    }
}
