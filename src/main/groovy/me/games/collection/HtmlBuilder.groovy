package me.games.collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('htmlBuilder')
class HtmlBuilder {

    @Autowired
    BGGScraper bggScraper

    HtmlBuilder(BGGScraper bggScraper) {
        this.bggScraper = bggScraper
    }

    def build(String username, Integer size, Boolean showName, Boolean showUrl, Boolean shuffle, int overflow = 0, int repeat = 0, Boolean includePrevOwned = false) {
        def xml = bggScraper.fetchCollection(username)
        def games = xml.children()
                .findAll {
                    (it.status.@own == "1" || (includePrevOwned && it.status.@prevowned == "1")) && it.@objecttype == 'thing' && it.@subtype == 'boardgame'
                }
                .collect {
                    [name: it.name.text(), imageUrl: it.thumbnail.text(), id: it.@objectid]
                }

        if (shuffle) {
            Collections.shuffle(games)
        }

        games = games * (repeat + 1)

        def writer = new StringWriter()
        def markup = new groovy.xml.MarkupBuilder(writer)

        markup.html {
            head {
                title("$username's colelction")
                link(rel: "icon", type: "image/webp", href: "https://raw.githubusercontent.com/nobuddyorg/nobuddyorg.github.io/refs/heads/main/web-buddy/public/logos/gamegallery.webp")
                style(type: "text/css", """
                    * {
                      margin: 0;
                      padding: 0;
                    }

                    html {
                      overflow: auto;
                    }
                    ::-webkit-scrollbar {
                        width: 0px;
                        background: transparent;
                    }
                    html, body {
                        overflow-x: visible;
                    }

                    .flex-container {
                        display: flex;
                        flex-wrap: wrap;
                        flex-direction: row;
                        align-items: stretch;
                        align-content: stretch;
                        justify-content: flex-start;
                        gap: 0;
                        margin-left: -${overflow}px;
                    }

                    .image {
                        width: ${size}px;
                        height: ${size}px;
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                        padding-left: ${overflow}px;
                    }

                    body {
                        background-color: #999999;
                    }

                    img {
                        min-height:${size}px;
                        min-width:${size}px;
                        object-fit: cover;
                        x-overflow: hidden;
                        max-height: 100%;
                        max-width: 100%;
                        position: relative;
                        z-index: 999;
                    }

                    .overlay {
                        display: inline-block;
                        text-align: center;
                        position: relative;
                        z-index: 999;
                        bottom: calc(100%);
                        left: 0;
                        width: 100%;
                        color: #eeeeee;
                        text-shadow: 0px 0px 4px #cccccc;
                        font-size: ${size / 10};
                        background-color: rgba(100,100,100,0.5);
                    }

                    a {
                        text-decoration: none;
                    }

                    .bgg-watermark {
                        position: fixed;
                        bottom: ${size / 2}px;
                        right: ${size / 2}px;
                        z-index: 0;
                        opacity: 0.8;
                        pointer-events: none;
                    }

                    .bgg-watermark img {
                        height: auto;
                        width: auto;
                        min-height: unset;
                        min-width: unset;
                        max-height: unset;
                        max-width: unset;
                        object-fit: contain;
                    }

                    #settings-hotzone {
                        position: fixed;
                        top: 0;
                        right: 0;
                        width: 90px;
                        height: 90px;
                        z-index: 10000;
                    }

                    #settings-toggle {
                        position: absolute;
                        top: 12px;
                        right: 12px;
                        width: 36px;
                        height: 36px;
                        border: none;
                        border-radius: 50%;
                        background-color: rgba(40, 40, 40, 0.8);
                        color: #eeeeee;
                        font-size: 1.1rem;
                        cursor: pointer;
                        opacity: 0;
                        pointer-events: none;
                        transition: opacity 0.15s ease;
                    }

                    #settings-hotzone:hover #settings-toggle,
                    #settings-hotzone.panel-open #settings-toggle {
                        opacity: 1;
                        pointer-events: auto;
                    }

                    #settings-panel {
                        display: none;
                        position: fixed;
                        top: 56px;
                        right: 12px;
                        z-index: 10000;
                        width: 220px;
                        padding: 16px;
                        border-radius: 8px;
                        background-color: rgba(40, 40, 40, 0.92);
                        color: #eeeeee;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        font-size: 0.8rem;
                    }

                    #settings-panel.open {
                        display: block;
                    }

                    #settings-panel label {
                        display: block;
                        margin-bottom: 3px;
                        color: #cfcfcf;
                    }

                    #settings-panel .field {
                        margin-bottom: 10px;
                    }

                    #settings-panel input[type="text"],
                    #settings-panel input[type="number"] {
                        width: 100%;
                        padding: 5px 6px;
                        border-radius: 4px;
                        border: 1px solid #555555;
                        background-color: #2b2b2b;
                        color: #eeeeee;
                    }

                    #settings-panel .checkbox-field {
                        display: flex;
                        align-items: center;
                        gap: 6px;
                        margin-bottom: 8px;
                    }

                    #settings-panel .checkbox-field label {
                        margin-bottom: 0;
                    }

                    #settings-panel button {
                        width: 100%;
                        margin-top: 4px;
                        padding: 7px;
                        border: none;
                        border-radius: 4px;
                        background-color: #4f7cff;
                        color: #ffffff;
                        font-weight: 600;
                        cursor: pointer;
                    }

                    #settings-panel button.secondary {
                        margin-top: 6px;
                        background-color: #555555;
                    }
                """)
            }

            body {
                div(class: "flex-container") {
                    games.each { game ->
                        def content = {
                            img(alt: game.name, title: showName ? '' : game.name, src: game.imageUrl)
                            if (showName) span(class: 'overlay', game.name)
                        }

                        div(class: 'image') {
                            if (showUrl) {
                                a(href: "https://boardgamegeek.com/boardgame/${game.id}", target: "_blank") {
                                    content()
                                }
                            } else {
                                content()
                            }
                        }
                    }
                }

                div(class: "bgg-watermark") {
                    img(src: "/powered-by-bgg.png", alt: "Powered by BoardGameGeek")
                }

                div(id: "settings-hotzone") {
                    button(id: "settings-toggle", type: "button", onclick: "document.getElementById('settings-panel').classList.toggle('open'); document.getElementById('settings-hotzone').classList.toggle('panel-open')", "⚙")
                }

                div(id: "settings-panel") {
                    form(id: "settings-form") {
                        div(class: "field") {
                            label(for: "s-username", "Username")
                            input(type: "text", id: "s-username", name: "username", value: username, required: "required")
                        }
                        div(class: "field") {
                            label(for: "s-size", "Cover Size (px)")
                            input(type: "number", id: "s-size", name: "size", value: size, min: "20")
                        }
                        div(class: "field") {
                            label(for: "s-overflow", "Overflow (px)")
                            input(type: "number", id: "s-overflow", name: "overflow", value: overflow, min: "0")
                        }
                        div(class: "field") {
                            label(for: "s-repeat", "Repeat")
                            input(type: "number", id: "s-repeat", name: "repeat", value: repeat, min: "0")
                        }

                        def showNameAttrs = [type: "checkbox", id: "s-showName", name: "showName"]
                        if (showName) showNameAttrs.checked = "checked"
                        div(class: "checkbox-field") {
                            input(showNameAttrs)
                            label(for: "s-showName", "Show names")
                        }

                        def showUrlAttrs = [type: "checkbox", id: "s-showUrl", name: "showUrl"]
                        if (showUrl) showUrlAttrs.checked = "checked"
                        div(class: "checkbox-field") {
                            input(showUrlAttrs)
                            label(for: "s-showUrl", "Link to BGG")
                        }

                        def shuffleAttrs = [type: "checkbox", id: "s-shuffle", name: "shuffle"]
                        if (shuffle) shuffleAttrs.checked = "checked"
                        div(class: "checkbox-field") {
                            input(shuffleAttrs)
                            label(for: "s-shuffle", "Shuffle")
                        }

                        def includePrevOwnedAttrs = [type: "checkbox", id: "s-includePrevOwned", name: "includePrevOwned"]
                        if (includePrevOwned) includePrevOwnedAttrs.checked = "checked"
                        div(class: "checkbox-field") {
                            input(includePrevOwnedAttrs)
                            label(for: "s-includePrevOwned", "Include prev. owned")
                        }

                        button(type: "submit", "Apply")
                        button(type: "button", class: "secondary", onclick: "window.location.href='/'", "Home")
                    }
                }

                script(type: "text/javascript", """
                    document.getElementById('settings-form').addEventListener('submit', function (evt) {
                        evt.preventDefault();
                        var f = evt.target;
                        var params = new URLSearchParams();
                        params.set('username', f.username.value.trim());
                        params.set('size', f.size.value);
                        params.set('showName', f.showName.checked);
                        params.set('showUrl', f.showUrl.checked);
                        params.set('shuffle', f.shuffle.checked);
                        params.set('overflow', f.overflow.value);
                        params.set('repeat', f.repeat.value);
                        params.set('includePrevOwned', f.includePrevOwned.checked);
                        window.location.href = '/collection?' + params.toString();
                    });
                """)
            }
        }

        writer.toString()
    }
}
