package me.games.collection

import spock.lang.Specification
import spock.lang.Subject
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult // Added for potentially more specific parsing

class HtmlBuilderSpec extends Specification {

    HtmlBuilder htmlBuilder
    // Use BGGScraper as the type for the mock for clarity
    BGGScraper bggScraper = Mock(BGGScraper)

    def setup() {
        htmlBuilder = new HtmlBuilder(bggScraper)
    }

    def "build - generates HTML with games and correct filtering"() {
        given:
            def mockXmlPayload = '''<items termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="1" subtype="boardgame">
                    <name type="primary" sortindex="1">Game 1</name>
                    <thumbnail>https://example.com/game1.jpg</thumbnail>
                    <status own="1" prevowned="0" fortrade="0" want="0" wanttoplay="0" wanttobuy="0" wishlist="0" preordered="0" lastmodified="2023-01-01 00:00:00"/>
                </item>
                <item objecttype="thing" objectid="2" subtype="boardgame">
                    <name type="primary" sortindex="1">Game 2</name>
                    <thumbnail>https://example.com/game2.jpg</thumbnail>
                    <status own="1" prevowned="0" fortrade="0" want="0" wanttoplay="0" wanttobuy="0" wishlist="0" preordered="0" lastmodified="2023-01-01 00:00:00"/>
                </item>
                <item objecttype="other" objectid="3" subtype="boardgame"> <!-- Should be filtered: objecttype != 'thing' -->
                    <name type="primary" sortindex="1">Game 3 (Not a 'thing')</name>
                    <thumbnail>https://example.com/game3.jpg</thumbnail>
                    <status own="1" prevowned="0" fortrade="0" want="0" wanttoplay="0" wanttobuy="0" wishlist="0" preordered="0" lastmodified="2023-01-01 00:00:00"/>
                </item>
                <item objecttype="thing" objectid="4" subtype="boardgame"> <!-- Should be filtered: status own != '1' -->
                    <name type="primary" sortindex="1">Game 4 (Not owned)</name>
                    <thumbnail>https://example.com/game4.jpg</thumbnail>
                    <status own="0" prevowned="0" fortrade="0" want="0" wanttoplay="0" wanttobuy="0" wishlist="0" preordered="0" lastmodified="2023-01-01 00:00:00"/>
                </item>
                <item objecttype="thing" objectid="5" subtype="boardgameexpansion"> <!-- Should be filtered: subtype != 'boardgame' -->
                    <name type="primary" sortindex="1">Game 5 (Expansion)</name>
                    <thumbnail>https://example.com/game5.jpg</thumbnail>
                    <status own="1" prevowned="0" fortrade="0" want="0" wanttoplay="0" wanttobuy="0" wishlist="0" preordered="0" lastmodified="2023-01-01 00:00:00"/>
                </item>
              </items>'''
            GPathResult mockParsedXml = new XmlSlurper().parseText(mockXmlPayload)
            bggScraper.fetchCollection("testuser") >> mockParsedXml

        when:
            // Parameters: username, size, showName, showUrl, shuffle, overflow = 0, repeat = 0
            def result = htmlBuilder.build("testuser", 200, true, true, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)

        then:
            1 * bggScraper.fetchCollection("testuser")
            parsedHtml.head.title.text() == "testuser's colelction" // Typo "colelction" is in source

            def images = parsedHtml.body.'div'.'div'.img
            images.size() == 2 // Only Game 1 and Game 2 should pass filters
            images[0].@alt.text() == "Game 1"
            images[1].@alt.text() == "Game 2"

            result.contains("href='https://boardgamegeek.com/boardgame/1'")
            result.contains("href='https://boardgamegeek.com/boardgame/2'")
            !result.contains("Game 3")
            !result.contains("Game 4")
            !result.contains("Game 5")
    }

    def "build - handles empty collection gracefully"() {
        given:
            def mockXml = new XmlSlurper().parseText('''<items></items>''')
            bggScraper.fetchCollection("testuser") >> mockXml

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)

        then:
            1 * bggScraper.fetchCollection("testuser")
            parsedHtml.body.'div'.'div'.size() == 0 // No image divs
            !result.contains("<div class='image'>") // Alternative check
    }

    def "build - hides name overlay when showName is false"() {
        given:
            def mockXmlPayload = '''<items>
            <item objectid="1" objecttype="thing" subtype="boardgame"><status own="1"/><name>Game 1</name><thumbnail>g1.jpg</thumbnail></item>
            </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXmlPayload)

        when:
            def result = htmlBuilder.build("testuser", 200, false, true, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)

        then:
            1 * bggScraper.fetchCollection("testuser")
            parsedHtml.body.'div'.'div'.a.span.size() == 0 // No span for overlay
            !result.contains("<span class='overlay'>Game 1</span>")
            parsedHtml.body.'div'.'div'.a.img.@title == "Game 1" // Name should be in title attribute of img
    }

    def "build - hides URL when showUrl is false"() {
        given:
            def mockXmlPayload = '''<items>
            <item objectid="1" objecttype="thing" subtype="boardgame"><status own="1"/><name>Game 1</name><thumbnail>g1.jpg</thumbnail></item>
            </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXmlPayload)

        when:
            def result = htmlBuilder.build("testuser", 200, true, false, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)

        then:
            1 * bggScraper.fetchCollection("testuser")
            parsedHtml.body.'div'.'div'.a.size() == 0 // No anchor tag
            parsedHtml.body.'div'.'div'.img.size() == 1 // Image should still be there
            !result.contains("<a href=")
    }

    def "build - generates correct image size and font size based on size parameter"() {
        given:
            def mockXmlPayload = '''<items>
            <item objectid="1" objecttype="thing" subtype="boardgame"><status own="1"/><name>Game 1</name><thumbnail>g1.jpg</thumbnail></item>
            </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXmlPayload)
            int customSize = 300

        when:
            def result = htmlBuilder.build("testuser", customSize, true, true, false, 0, 0)

        then:
            1 * bggScraper.fetchCollection("testuser")
            result.contains(".image {\n                        width: ${customSize}px;")
            result.contains("height: ${customSize}px;")
            result.contains("min-height:${customSize}px;")
            result.contains("min-width:${customSize}px;")
            result.contains("font-size: ${customSize / 10};")
    }

    def "build - shuffles games when shuffle flag is true"() {
        given: "A list of many games to make coincidental same order unlikely"
            def gameItems = (1..50).collect { i ->
                """<item objectid="${i}" objecttype="thing" subtype="boardgame">
                     <status own="1"/>
                     <name>Game ${i}</name>
                     <thumbnail>https://example.com/game${i}.jpg</thumbnail>
                   </item>"""
            }.join('')
            def mockXmlPayload = "<items>${gameItems}</items>"
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXmlPayload)

        when: "Building HTML with and without shuffle"
            // Note: shuffle happens on the list derived from XML *before* repeat is applied.
            def resultNoShuffle = htmlBuilder.build("testuser", 200, true, true, false, 0, 0)
            def resultWithShuffle = htmlBuilder.build("testuser", 200, true, true, true, 0, 0)

            def gamesNoShuffle = extractGameOrderFromHtml(resultNoShuffle)
            def gamesWithShuffle = extractGameOrderFromHtml(resultWithShuffle)

        then: "The order of games should be different"
        1 * bggScraper.fetchCollection("testuser") // For no shuffle
        1 * bggScraper.fetchCollection("testuser") // For with shuffle
        gamesNoShuffle.size() == 50
        gamesWithShuffle.size() == 50
        gamesNoShuffle != gamesWithShuffle // High probability for 50 items

        and: "Verify all original games are present in shuffled list"
        gamesNoShuffle.toSet() == gamesWithShuffle.toSet()
    }

    def "build - repeats games when repeat parameter is greater than 0"() {
        given:
            def mockXmlPayload = '''<items>
                <item objectid="1" objecttype="thing" subtype="boardgame"><status own="1"/><name>Game 1</name><thumbnail>g1.jpg</thumbnail></item>
                <item objectid="2" objecttype="thing" subtype="boardgame"><status own="1"/><name>Game 2</name><thumbnail>g2.jpg</thumbnail></item>
            </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXmlPayload)
            int repeatCount = 2 // This means original + 2 repetitions = 3 times total

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, 0, repeatCount)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)

        then:
            1 * bggScraper.fetchCollection("testuser")
            def images = parsedHtml.body.'div'.'div'.a.img
            images.size() == 2 * (repeatCount + 1) // Original games * (repeat + 1)

            // Check for specific game repetition pattern
            images[0].@alt.text() == "Game 1"
            images[1].@alt.text() == "Game 2"
            images[2].@alt.text() == "Game 1"
            images[3].@alt.text() == "Game 2"
            images[4].@alt.text() == "Game 1"
            images[5].@alt.text() == "Game 2"
    }

    def "build - applies overflow CSS properties correctly"() {
        given:
            def mockXmlPayload = '''<items>
                <item objectid="1" objecttype="thing" subtype="boardgame"><status own="1"/><name>Game 1</name><thumbnail>g1.jpg</thumbnail></item>
            </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXmlPayload)
            int overflowValue = 15

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, overflowValue, 0)

        then:
            1 * bggScraper.fetchCollection("testuser")
            // Check CSS strings for overflow values
            // The CSS for flex-container margin-left is negative
            result.contains("margin-left: -${overflowValue}px;")
            // The CSS for image padding-left is positive
            result.contains("padding-left: ${overflowValue}px;")
    }

    // Helper method from existing tests, adapted slightly
    private List<String> extractGameOrderFromHtml(String html) {
        def parsed = new XmlSlurper().parseText(html)
        return parsed.body.div.div.depthFirst().grep { it.name() == 'img' }*.@alt*.text()
    }

    // Removed the large number of games test as it's implicitly covered by shuffle and repeat tests.
    // Removed the println statements from the original tests.
}
