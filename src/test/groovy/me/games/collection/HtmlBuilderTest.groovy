package me.games.collection

import spock.lang.Specification
import spock.lang.Unroll

class HtmlBuilderTest extends Specification {

    @Unroll
    def "Test 'Build' function creates correct html output"() {
        given:
            HtmlBuilder htmlBuilder = new HtmlBuilder()
            BGGScraper mockScraper = Mock(BGGScraper)
            def xmlParsed = new XmlSlurper().parseText(SpecHelper.getResourceContent('/mockResponse.xml'))
            mockScraper.fetchCollection(username) >> xmlParsed
            htmlBuilder.setBggScraper(mockScraper)

        when:
            def outs = [].toSet()
            String output = htmlBuilder.build(username, size, showName, showUrl, shuffle)
            (1..10).each {
                outs << htmlBuilder.build(username, size, showName, showUrl, shuffle)
            }

            def xmlOutput = new XmlSlurper().parseText(output)

        then:
            xmlOutput.head.title == "$username's colelction"
            xmlOutput.body.div.div.size() == 3 // although 4 in response, 1 not owned
            xmlOutput.head.toString().contains("width: ${size}px")
            showName ? output.contains("<span class='overlay'>") : true
            showUrl ? output.contains("<a href='https://boardgamegeek.com/boardgame/") : true

            shuffle ? outs.size() > 1 : outs.size() == 1

        where:
            username | size | showName | showUrl | shuffle
            "123"    | 20   | true     | false   | false
            "123"    | 20   | false    | true    | false
            "123"    | 20   | false    | false   | true
            "321"    | 40   | false    | false   | false
    }
}
