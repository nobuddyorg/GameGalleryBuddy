package me.games.collection

import spock.lang.Specification
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

class HtmlBuilderSpec extends Specification {

    HtmlBuilder htmlBuilder
    BGGScraper actualBggScraper // Using a real instance + metaClass
    int fetchCollectionCallCount // Manual counter for interactions

    def setup() {
        actualBggScraper = new BGGScraper()
        actualBggScraper.searchBase = "https://api.geekdo.com/xmlapi2"
        htmlBuilder = new HtmlBuilder(actualBggScraper)
        fetchCollectionCallCount = 0
    }

    // Helper to stub fetchCollection and count calls
    private void stubFetchCollection(Closure listProvider) {
        actualBggScraper.metaClass.fetchCollection = { String usernameArg ->
            fetchCollectionCallCount++
            // println "metaClass stub fetchCollection called with: $usernameArg, call count: $fetchCollectionCallCount" // Debug line removed
            listProvider.call(usernameArg)
        }
    }

    def "build - generates HTML with games and correct filtering"() {
        given:
            stubFetchCollection {
                [
                    [name: "Game 1", imageUrl: "https://example.com/game1.jpg", id: "1"],
                    [name: "Game 2", imageUrl: "https://example.com/game2.jpg", id: "2"]
                ]
            }

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false, 0, 0)
            GPathResult parsedHtml = new XmlSlurper().parseText(result)

        then:
            fetchCollectionCallCount == 1
            parsedHtml.head.title.text() == "testuser's colelction"

            def images = parsedHtml.body.'div'.'div'.a.img
            images.size() == 2
            images[0].@alt.text() == "Game 1"
            images[1].@alt.text() == "Game 2"

            result.contains("href='https://boardgamegeek.com/boardgame/1'")
            result.contains("href='https://boardgamegeek.com/boardgame/2'")
    }

    def "build - handles empty collection gracefully"() {
        given:
            stubFetchCollection { [] }

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
                [[name: "Game 1", imageUrl: "g1.jpg", id: "1"]]
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
                [[name: "Game 1", imageUrl: "g1.jpg", id: "1"]]
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
                [[name: "Game 1", imageUrl: "g1.jpg", id: "1"]]
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
            stubFetchCollection {
                (1..50).collect { i ->
                    [name: "Game ${i}", imageUrl: "https://example.com/game${i}.jpg", id: "${i}"]
                }
            }
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
            def baseGamesList = [
                [name: "Game 1", imageUrl: "g1.jpg", id: "1"],
                [name: "Game 2", imageUrl: "g2.jpg", id: "2"]
            ]
            stubFetchCollection { baseGamesList }
            int repeatCount = 2
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
                def originalGameIndex = i % baseGamesList.size()
                def currentImgTag = gameMap[i].a.img ?: gameMap[i].img
                assert currentImgTag.@alt.text() == baseGamesList[originalGameIndex].name
            }
    }

    def "build - applies overflow CSS properties correctly"() {
        given:
            stubFetchCollection {
                [[name: "Game 1", imageUrl: "g1.jpg", id: "1"]]
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
