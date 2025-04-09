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

    def build(String username, Integer size, Boolean showName, Boolean showUrl, Boolean shuffle) {
        def xml = bggScraper.fetchCollection(username)
        def games = xml.children()
                .findAll {
                    it.status.@own == "1" && it.@objecttype == 'thing' && it.@subtype == 'boardgame'
                }
                .collect {
                    [name: it.name.text(), imageUrl: it.thumbnail.text(), id: it.@objectid]
                }

        if (shuffle) {
            Collections.shuffle(games)
        }

        def writer = new StringWriter()
        def markup = new groovy.xml.MarkupBuilder(writer)

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
                    
                    .flex-container {
                        display: flex;
                        flex-wrap: wrap;
                        flex-direction: row;
                        align-items: stretch;
                        align-content: stretch;
                        justify-content: space-between;
                    }
                    
                    .image {
                        width: ${size}px;
                        height: ${size}px;
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
                    games.each { game ->
                        div(class: 'image') {
                            a(href: showUrl ? "https://boardgamegeek.com/boardgame/${game.id}" : '', target: "_blank") {
                                img(alt: game.name, title: showName ? '' : game.name, src: game.imageUrl)
                                if (showName) span(class: 'overlay', game.name)
                            }
                        }
                    }
                }
            }
        }

        writer.toString()
    }
}
