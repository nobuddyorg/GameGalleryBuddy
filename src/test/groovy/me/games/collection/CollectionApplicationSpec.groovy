package me.games.collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CollectionApplicationSpec {

    @Autowired
    WebApplicationContext context

    @Autowired
    TestRestTemplate restTemplate

    MockMvc mockMvc

    @Test
    void contextLoads() {
        assert context != null
    }

    @Test
    void testControllerLoaded() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
        mockMvc.perform(get("/collection?username=john"))
                .andExpect(status().isOk())
    }

    @Test
    void testFetchCollectionWithRestTemplate() {
        def response = restTemplate.getForEntity("/collection?username=john", String)
        assert response.statusCode == HttpStatus.OK
    }

    @Test
    void mainMethodIsCovered() {
        CollectionApplication.main(new String[0])
    }
}
