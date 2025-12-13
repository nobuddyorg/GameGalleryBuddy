# 🎲 GameGalleryBuddy

**GameGalleryBuddy** generates a wallpaper using all board games from a specified [BoardGameGeek](https://boardgamegeek.com) user’s collection.

![Powered by BGG](src/main/resources/static/powered-by-bgg.png)

## Getting Started

Run the application with:

```bash
./run.sh
```

Then, open your browser and go to:

```
http://localhost:8080/collection
```

## Parameters

| Parameter          | Required | Default | Description                                                                              |
| ------------------ | -------- | ------- | ---------------------------------------------------------------------------------------- |
| `username`         | ✅ Yes   | —       | BGG username of the collection owner.                                                    |
| `size`             | ❌ No    | `150`   | Size (in pixels) of each board game cover on the wallpaper.                              |
| `showName`         | ❌ No    | `false` | Whether to display the game name as an overlay on the image.                             |
| `showUrl`          | ❌ No    | `true`  | Whether to make game images clickable, linking to their BGG pages.                       |
| `shuffle`          | ❌ No    | `false` | Shuffle the games randomly (if `true`) or keep their original order.                     |
| `overflow`         | ❌ No    | `0`     | Allows images to overflow the container edges, in pixels.                                |
| `repeat`           | ❌ No    | `0`     | Repeats the image list `(repeat + 1)` times to extend the wallpaper.                     |
| `includePrevOwned` | ❌ No    | `false` | Also include games marked as _previously owned_ on BGG. Owned games are always included. |

## Example

Open the following URL in your browser to see an example:

```
http://localhost:8080/collection?username=besessener&size=85&showName=no&showUrl=no&shuffle=yes&overflow=20&repeat=1
```

## Sample Output

![Wall preview](https://user-images.githubusercontent.com/8039350/124144974-b876c480-da8c-11eb-9cc0-76a2c350bf6b.png)
