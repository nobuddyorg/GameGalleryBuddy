package me.games.collection

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import spock.lang.Specification

class GameRestControllerSpec extends Specification {

    def htmlBuilder = Mock(HtmlBuilder)
    def controller = new GameRestController(htmlBuilder)

    def setup() {
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response))
    }

    def "fetchCollection - all params customized"() {
        given:
            htmlBuilder.build("alice", 42, true, false, true) >> "<html>Custom</html>"

        when:
            def result = controller.fetchCollection("alice", 42, true, false, true)

        then:
            result == "<html>Custom</html>"
    }
}
