package me.games.collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
class GameRestController {
    @Autowired
    HtmlBuilder htmlBuilder

    GameRestController(HtmlBuilder htmlBuilder) {
        this.htmlBuilder = htmlBuilder
    }

    @GetMapping('/collection')
    String fetchCollection(
            @RequestParam(name = "username") String username,
            @RequestParam(name = "size", required = false, defaultValue = "150") Integer size,
            @RequestParam(name = "showName", required = false, defaultValue = "false") Boolean showName,
            @RequestParam(name = "showUrl", required = false, defaultValue = "true") Boolean showUrl,
            @RequestParam(name = "shuffle", required = false, defaultValue = "false") Boolean shuffle,
            @RequestParam(name = "overflow", required = false, defaultValue = "0") Integer overflow,
            @RequestParam(name = "repeat", required = false, defaultValue = "0") Integer repeat,
            @RequestParam(name = "includePrevOwned", required = false, defaultValue = "false") Boolean includePrevOwned) {
        htmlBuilder.build(username, size, showName, showUrl, shuffle, overflow, repeat, includePrevOwned)
    }
}
