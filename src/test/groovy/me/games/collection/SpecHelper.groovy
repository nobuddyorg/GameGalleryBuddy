package me.games.collection

class SpecHelper {
    static getResourceContent(name) {
        getClass().getResource(name).text
    }
}
