package me.games.collection


import org.springframework.stereotype.Component

@Component('bggScraper')
class BGGScraper {

    def fetchCollection(String username) {

//        System.getProperties().put('proxySet', 'true');
//        System.getProperties().put('proxyHost', 'hostname');
//        System.getProperties().put('proxyPort', '3128');

        String searchBase = 'http://api.geekdo.com/xmlapi2'
        String searchQuery = 'collection'
        String searchParameter = 'username'

        String queueMessage = 'Please try again later for access'
        String content = queueMessage

        while (content.contains(queueMessage)) {
            try {
                URL obj = "$searchBase/$searchQuery?$searchParameter=$username".toURL()
                HttpURLConnection conn = (HttpURLConnection) obj.openConnection()
                conn.setInstanceFollowRedirects(true)
                HttpURLConnection.setFollowRedirects(true)

                content = conn.getHeaderField("Location").toURL().text
                if (content.contains(queueMessage)) sleep(1 * 1000)
            } catch (e) {
                sleep(5 * 1000)
            }
        }

        return new XmlSlurper().parseText(content)
    }
}
