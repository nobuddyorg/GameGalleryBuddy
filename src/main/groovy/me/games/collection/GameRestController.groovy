package me.games.collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
class GameRestController {
    @Autowired
    HtmlBuilder htmlBuilder

    @GetMapping('/collection')
    String fetchCollection(
            @RequestParam(name = "username") String username,
            @RequestParam(name = "size", required = false, defaultValue = "150") Integer size,
            @RequestParam(name = "showName", required = false, defaultValue = "false") Boolean showName,
            @RequestParam(name = "showUrl", required = false, defaultValue = "true") Boolean showUrl,
            @RequestParam(name = "shuffle", required = false, defaultValue = "false") Boolean shuffle) {
        htmlBuilder.build(username, size, showName, showUrl, shuffle)
    }
}
