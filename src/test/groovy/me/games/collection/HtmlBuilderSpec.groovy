package me.games.collection

import spock.lang.Specification
import spock.lang.Subject
import groovy.xml.XmlSlurper

class HtmlBuilderSpec extends Specification {

    HtmlBuilder htmlBuilder
    def bggScraper = Mock(BGGScraper)

    def setup() {
        htmlBuilder = new HtmlBuilder(bggScraper)
    }

    def "build - generates HTML with games"() {
        given:
            def mockXml = '''<items>
                <item objectid="1" objecttype="thing" subtype="boardgame">
                    <status own="1"/>
                    <name>Game 1</name>
                    <thumbnail>https://example.com/game1.jpg</thumbnail>
                </item>
                <item objectid="2" objecttype="thing" subtype="boardgame">
                    <status own="1"/>
                    <name>Game 2</name>
                    <thumbnail>https://example.com/game2.jpg</thumbnail>
                </item>
                <item objectid="3" objecttype="other" subtype="boardgame">
                    <status own="1"/>
                    <name>Game 3</name>
                    <thumbnail>https://example.com/game3.jpg</thumbnail>
                </item>
                <item objectid="4" objecttype="thing" subtype="boardgame">
                    <status own="0"/>
                    <name>Game 4</name>
                    <thumbnail>https://example.com/game4.jpg</thumbnail>
                </item>
                <item objectid="5" objecttype="thing" subtype="other">
                    <status own="1"/>
                    <name>Game 5</name>
                    <thumbnail>https://example.com/game5.jpg</thumbnail>
                </item>
              </items>'''

            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXml)

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false)

        then:
            println(result)

            result.contains("<title>testuser's colelction</title>")
            result.contains("<img alt='Game 1' title='' src='https://example.com/game1.jpg'")
            result.contains("<img alt='Game 2' title='' src='https://example.com/game2.jpg'")
            result.contains("href='https://boardgamegeek.com/boardgame/1'")
            result.contains("href='https://boardgamegeek.com/boardgame/2'")
    }

    def "build - handles empty collection gracefully"() {
        given:
            def mockXml = '''<items></items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXml)

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false)

        then:
            println(result)
            result.contains("<body>")
            result.contains("</body>")
            !result.contains("<div class='image'>")
    }

    def "build - hides name overlay when showName is false"() {
        given:
            def mockXml = '''<items>
            <item objectid="1" objecttype="thing" subtype="boardgame">
                <status own="1"/>
                <name>Game 1</name>
                <thumbnail>https://example.com/game1.jpg</thumbnail>
            </item>
          </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXml)

        when:
            def result = htmlBuilder.build("testuser", 200, false, true, false)

        then:
            println(result)
            !result.contains("<span class='overlay'>Game 1</span>")
    }

    def "build - hides URL when showUrl is false"() {
        given:
            def mockXml = '''<items>
            <item objectid="1" objecttype="thing" subtype="boardgame">
                <status own="1"/>
                <name>Game 1</name>
                <thumbnail>https://example.com/game1.jpg</thumbnail>
            </item>
          </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXml)

        when:
            def result = htmlBuilder.build("testuser", 200, true, false, false)

        then:
            println(result)
            !result.contains("<a href=")
    }

    def "build - handles large number of games"() {
        given:
            def mockXml = '''<items>'''
            (1..100).each { i ->
                mockXml += """<item objectid="${i}" objecttype="thing" subtype="boardgame">
                            <status own="1"/>
                            <name>Game ${i}</name>
                            <thumbnail>https://example.com/game${i}.jpg</thumbnail>
                          </item>"""
            }
            mockXml += '</items>'

            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXml)

        when:
            def result = htmlBuilder.build("testuser", 200, true, true, false)

        then:
            println(result)
            (1..100).each { i ->
                result.contains("<img alt='Game ${i}' src='https://example.com/game${i}.jpg'")
            }
    }

    def "build - generates correct image size based on size parameter"() {
        given:
            def mockXml = '''<items>
            <item objectid="1" objecttype="thing" subtype="boardgame">
                <status own="1"/>
                <name>Game 1</name>
                <thumbnail>https://example.com/game1.jpg</thumbnail>
            </item>
          </items>'''
            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXml)

        when:
            def result = htmlBuilder.build("testuser", 300, true, true, false)

        then:
            println(result)
            result.contains('width: 300px')
            result.contains('height: 300px')
    }

    def "build - shuffles games when shuffle flag is true"() {
        given:
            def mockXml = '''<items>'''
            (1..250).each { i ->
                mockXml += """<item objectid="${i}" objecttype="thing" subtype="boardgame">
                            <status own="1"/>
                            <name>Game ${i}</name>
                            <thumbnail>https://example.com/game${i}.jpg</thumbnail>
                          </item>"""
            }
            mockXml += '</items>'

            bggScraper.fetchCollection("testuser") >> new XmlSlurper().parseText(mockXml)

        when:
            def resultBeforeShuffle = htmlBuilder.build("testuser", 200, true, true, false)
            def resultAfterShuffle = htmlBuilder.build("testuser", 200, true, true, true)

            def gamesBeforeShuffle = extractGameOrder(resultBeforeShuffle)
            def gamesAfterShuffle = extractGameOrder(resultAfterShuffle)

        then:
            assert gamesBeforeShuffle != gamesAfterShuffle
    }

    def extractGameOrder(String html) {
        def gameOrder = []
        def matcher = (html =~ /<img alt='([^']+)'/)
        matcher.each { match ->
            gameOrder << match[1]
        }
        return gameOrder
    }
}
