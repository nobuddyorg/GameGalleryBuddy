package me.games.collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class CollectionApplication {

    static void main(String[] args) {
        SpringApplication.run(CollectionApplication, args)
    }

}

@RestController
class GameRestController {
    @Autowired
    HtmlBuilder htmlBuilder

    @GetMapping('/collection')
    String fetchCollection(
            @RequestParam(required = true) String username,
            @RequestParam(required = false, defaultValue = "150") Integer size,
            @RequestParam(required = false, defaultValue = "false") Boolean showName,
            @RequestParam(required = false, defaultValue = "true") Boolean showUrl,
            @RequestParam(required = false, defaultValue = "false") Boolean shuffle) {
        htmlBuilder.build(username, size, showName, showUrl, shuffle)
    }
}
