package me.games.collection

import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@SpringBootTest(classes = CollectionApplication.class)
class GameRestControllerTest extends Specification {

    @SpringBean
    private HtmlBuilder htmlBuilder = Stub()

    @Autowired
    private MockMvc mvc

    def "when get is performed then the response has status 200 and content is 'Hello world!'"() {
        given:
            htmlBuilder.build('123', 150, false, true, false) >> 'Hello world!'

        expect: "Status is 200 and the response is 'Hello world!'"
            mvc.perform(get('/collection').param('username', '123'))
                    .andExpect(status().isOk())
                    .andReturn()
                    .response
                    .contentAsString == 'Hello world!'
    }
}
