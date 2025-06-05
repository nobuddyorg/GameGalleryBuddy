package me.games.collection

import spock.lang.Specification
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

class HtmlBuilderSpec extends Specification {

    HtmlBuilder htmlBuilder
    BGGScraper actualBggScraper // Using a real instance + metaClass
    int fetchCollectionCallCount // Manual counter for interactions
    XmlSlurper testXmlSlurper // Used to create GPathResult for the stub

    def setup() {
        actualBggScraper = new BGGScraper()
        actualBggScraper.searchBase = "https://api.geekdo.com/xmlapi2"
        htmlBuilder = new HtmlBuilder(actualBggScraper)
        fetchCollectionCallCount = 0
        testXmlSlurper = new XmlSlurper()
    }

    // Helper to stub fetchCollection and count calls
    private void stubFetchCollection(Closure xmlProvider) {
        actualBggScraper.metaClass.fetchCollection = { String usernameArg ->
            fetchCollectionCallCount++
            // The closure now provides an XML string, which is parsed here to return GPathResult
            testXmlSlurper.parseText(xmlProvider.call(usernameArg))
        }
    }

    def "build - generates HTML with games and correct filtering"() {
        given:
            stubFetchCollection {
                '''
                <items totalitems="2" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                    <item objecttype="thing" objectid="1" subtype="boardgame">
                        <name type="primary">Game 1</name>
                        <thumbnail>https://example.com/game1.jpg</thumbnail>
                        <status own="1"/>
                    </item>
                    <item objecttype="thing" objectid="2" subtype="boardgame">
                        <name type="primary">Game 2 (Not Owned)</name>
                        <thumbnail>https://example.com/game2.jpg</thumbnail>
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
                </items>
                '''
            }

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)

        then:
            fetchCollectionCallCount == 1
            parsedHtml.head.title.text() == "testuser's colelction"

            def images = parsedHtml.body.'div'.'div'.a.img
            images.size() == 2 // Game 1 and Game 4 should pass the filter
            images[0].@alt.text() == "Game 1"
            images[1].@alt.text() == "Game 4"

            result.contains("href='https://boardgamegeek.com/boardgame/1'")
            result.contains("href='https://boardgamegeek.com/boardgame/4'")
            !result.contains("Game 2 (Not Owned)")
            !result.contains("Game 3 (Expansion)")
    }

    def "build - handles empty collection gracefully"() {
        given:
            stubFetchCollection { "<items totalitems=\"0\"></items>" }

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)
        then:
            fetchCollectionCallCount == 1
            parsedHtml.body.'div'.'div'.size() == 0
            !result.contains("<div class='image'>")
    }

    def "build - hides name overlay when showName is false"() {
        given:
            stubFetchCollection {
                '''<items>
                     <item objecttype="thing" objectid="1" subtype="boardgame">
                       <name>Game 1</name><thumbnail>g1.jpg</thumbnail><status own="1"/>
                     </item>
                   </items>'''
            }
        when:
            def result = htmlBuilder.build("testuser", 200, false, true, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)
        then:
            fetchCollectionCallCount == 1
            parsedHtml.body.'div'.'div'.a.span.size() == 0
            !result.contains("<span class='overlay'>Game 1</span>")
            parsedHtml.body.'div'.'div'.a.img.@title == "Game 1"
    }

    def "build - hides URL when showUrl is false"() {
        given:
            stubFetchCollection {
                '''<items>
                     <item objecttype="thing" objectid="1" subtype="boardgame">
                       <name>Game 1</name><thumbnail>g1.jpg</thumbnail><status own="1"/>
                     </item>
                   </items>'''
            }
        when:
            def result = htmlBuilder.build("testuser", 200, true, false, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)
        then:
            fetchCollectionCallCount == 1
            parsedHtml.body.'div'.'div'.a.size() == 0
            parsedHtml.body.'div'.'div'.img.size() == 1
            !result.contains("<a href=")
    }

    def "build - generates correct image size and font size based on size parameter"() {
        given:
            stubFetchCollection {
                '''<items>
                     <item objecttype="thing" objectid="1" subtype="boardgame">
                       <name>Game 1</name><thumbnail>g1.jpg</thumbnail><status own="1"/>
                     </item>
                   </items>'''
            }
            int customSize = 300
        when:
            def result = htmlBuilder.build("testuser", customSize, true, true, false, 0, 0)
        then:
            fetchCollectionCallCount == 1
            result.contains(".image {\n                        width: ${customSize}px;")
            result.contains("height: ${customSize}px;")
            result.contains("min-height:${customSize}px;")
            result.contains("min-width:${customSize}px;")
            result.contains("font-size: ${customSize / 10};")
    }

    def "build - shuffles games when shuffle flag is true"() {
        given:
            def gameItemsXml = (1..50).collect { i ->
                """<item objecttype="thing" objectid="${i}" subtype="boardgame">
                     <status own="1"/>
                     <name>Game ${i}</name>
                     <thumbnail>https://example.com/game${i}.jpg</thumbnail>
                   </item>"""
            }.join('')
            def fullMockXml = "<items>${gameItemsXml}</items>"

            stubFetchCollection { fullMockXml }
        when:
            def resultNoShuffle = htmlBuilder.build("testuser", 200, true, true, false, 0, 0)
            def resultWithShuffle = htmlBuilder.build("testuser", 200, true, true, true, 0, 0)

            def gamesNoShuffle = extractGameOrderFromHtml(resultNoShuffle)
            def gamesWithShuffle = extractGameOrderFromHtml(resultWithShuffle)
        then:
            fetchCollectionCallCount == 2
            gamesNoShuffle.size() == 50
            gamesWithShuffle.size() == 50
            gamesNoShuffle != gamesWithShuffle
            gamesNoShuffle.toSet() == gamesWithShuffle.toSet()
    }

    def "build - repeats games when repeat parameter is greater than 0"() {
        given:
            def baseGamesXml = '''
                <items>
                    <item objecttype="thing" objectid="1" subtype="boardgame"><status own="1"/><name>Game 1</name><thumbnail>g1.jpg</thumbnail></item>
                    <item objecttype="thing" objectid="2" subtype="boardgame"><status own="1"/><name>Game 2</name><thumbnail>g2.jpg</thumbnail></item>
                </items>'''
            stubFetchCollection { baseGamesXml }
            int repeatCount = 2
            def expectedGames = [ // Expected names for assertion
                [name: "Game 1"],
                [name: "Game 2"]
            ]
        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, 0, repeatCount)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)
        then:
            fetchCollectionCallCount == 1
            def images = parsedHtml.body.div.div.a.img
            if (images.isEmpty()) {
                images = parsedHtml.body.div.div.img
            }
            images.size() == 2 * (repeatCount + 1)

            def gameMap = parsedHtml.body.div.div
            (0..<gameMap.size()).each { i ->
                def originalGameIndex = i % expectedGames.size()
                def currentImgTag = gameMap[i].a.img ?: gameMap[i].img
                assert currentImgTag.@alt.text() == expectedGames[originalGameIndex].name
            }
    }

    def "build - applies overflow CSS properties correctly"() {
        given:
            stubFetchCollection {
                '''<items>
                     <item objecttype="thing" objectid="1" subtype="boardgame">
                       <name>Game 1</name><thumbnail>g1.jpg</thumbnail><status own="1"/>
                     </item>
                   </items>'''
            }
            int overflowValue = 15
        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, overflowValue, 0)
        then:
            fetchCollectionCallCount == 1
            result.contains("margin-left: -${overflowValue}px;")
            result.contains("padding-left: ${overflowValue}px;")
    }

    private List<String> extractGameOrderFromHtml(String html) {
        def parsed = new XmlSlurper().parseText(html)
        def images = parsed.body.div.div.a.img
        if (images.isEmpty()) {
             images = parsed.body.div.div.img
        }
        return images*.@alt*.text()
    }
}
