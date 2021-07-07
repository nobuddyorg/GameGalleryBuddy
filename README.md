# BGGWallPaper
Creates a wall paper based with all games of a given user on BoardGameGeek.com

Run the spring boot application with `./gradlew bootRun`.
Access it on `http://localhost:8080/collection`

It requires some parameters to properly work:

* username (required: **yes**): BGG username of the user you want to see the collection of.
* size (required = false, defaultValue = "150"): size of each boardgames cover on the wall chart.
* showName (required = false, defaultValue = "false"): dispay game name as image overlay.
* showUrl (required = false, defaultValue = "true"): make images clickable by adding their links to BGG.
* shuffle (required = false, defaultValue = "false"): order alphabetically or randomly.

example: http://localhost:8080/collection?username=besessener&size=85&showName=no&showUrl=no&shuffle=yes

![image](https://user-images.githubusercontent.com/8039350/124144974-b876c480-da8c-11eb-9cc0-76a2c350bf6b.png)
