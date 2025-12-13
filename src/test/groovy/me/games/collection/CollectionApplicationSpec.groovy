package me.games.collection

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
class CollectionApplicationSpec {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    MockMvc mockMvc

    @Test
    void testControllerLoaded() {
        mockMvc.perform(get("/collection?username=john"))
                .andExpect(status().isOk())
    }

    @Test
    void testFetchCollectionWithRestTemplate() {
        def response = restTemplate.getForEntity("http://localhost:${port}/collection?username=john", String)
        assert response.statusCode == HttpStatus.OK
    }

    @Test
    void mainMethodIsCovered() {
        CollectionApplication.main(new String[0])
    }
}
