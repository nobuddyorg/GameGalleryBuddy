# 🎲 BGGWallPaper

**BGGWallPaper** generates a wallpaper using all board games from a specified [BoardGameGeek](https://boardgamegeek.com) user’s collection.

---

## 🚀 Getting Started

Run the application with:

```bash
./run.sh
```

Then, open your browser and go to:

```
http://localhost:8080/collection
```

---

## 🔧 Parameters

| Parameter   | Required | Default | Description                                                                 |
|-------------|----------|---------|-----------------------------------------------------------------------------|
| `username`  | ✅ Yes    | —       | BGG username of the collection owner.                                      |
| `size`      | ❌ No     | `150`   | Size (in pixels) of each board game cover on the wallpaper.                |
| `showName`  | ❌ No     | `false` | Whether to display the game name as an overlay on the image.               |
| `showUrl`   | ❌ No     | `true`  | Whether to make game images clickable, linking to their BGG pages.         |
| `shuffle`   | ❌ No     | `false` | Shuffle the games randomly (if `true`) or order them alphabetically.       |

---

## 🧪 Example

Open the following URL in your browser to see an example:

```
http://localhost:8080/collection?username=besessener&size=85&showName=no&showUrl=no&shuffle=yes
```

---

## 🖼️ Sample Output

![Wall preview](https://user-images.githubusercontent.com/8039350/124144974-b876c480-da8c-11eb-9cc0-76a2c350bf6b.png)
