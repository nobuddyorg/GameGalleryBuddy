package me.games.collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import groovy.xml.MarkupBuilder
import java.io.StringWriter
import java.util.Collections

@Component('htmlBuilder')
class HtmlBuilder {

    @Autowired
    BGGScraper bggScraper

    HtmlBuilder(BGGScraper bggScraper) {
        this.bggScraper = bggScraper
    }

    def build(String username, Integer size, Boolean showName, Boolean showUrl, Boolean shuffle, int overflow = 0, int repeat = 0) {
        List<Map<String, String>> gamesList = bggScraper.fetchCollection(username)

        // Defensive copy if shuffle is true, as Collections.shuffle works in-place
        List<Map<String, String>> processedGames = new ArrayList<>(gamesList)

        if (shuffle) {
            Collections.shuffle(processedGames)
        }

        if (repeat > 0 && !processedGames.isEmpty()) {
            List<Map<String, String>> originalGames = new ArrayList<>(processedGames)
            for (int i = 0; i < repeat; i++) {
                processedGames.addAll(originalGames)
            }
        }

        def writer = new StringWriter()
        def markup = new MarkupBuilder(writer)

        markup.html {
            head {
                title("$username's colelction")
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
                        background: transparent; /* make scrollbar transparent */
                    }
                    html, body {
                        overflow-x: visible; /* or scroll if you want a scrollbar */
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
                """)
            }

            body {
                div(class: "flex-container") {
                    processedGames.each { game ->
                        def contentClosure = {
                            img(alt: game.name, title: showName ? '' : game.name, src: game.imageUrl)
                            if (showName) span(class: 'overlay', game.name)
                        }

                        div(class: 'image') {
                            if (showUrl) {
                                a(href: "https://boardgamegeek.com/boardgame/${game.id}", target: "_blank") {
                                    contentClosure()
                                }
                            } else {
                                contentClosure()
                            }
                        }
                    }
                }
            }
        }

        writer.toString()
    }
}
