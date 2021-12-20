JSON messages in LibertyBans are based on RezzedUp's [json.sk](https://www.spigotmc.org/resources/json-sk.8851/). If you have used Skript's JSON.sk or otherwise already use json.sk formatting, you don't need to read this guide. All messages can use json.sk formatting.

### What is JSON in Minecraft? ###

JSON is a way of sending complex messages to players. JSON lets you send hoverable text, clickable links, and much more, putting dynamism in your chat with user-friendly messages.

You may be familiar with the `/tellraw` command. Using /tellraw, you can send JSON messages to other players. However, the format of tellraw is not the best. It looks like: `tellraw @a ["",{"text":"Basic","bold":true,"color":"yellow"},{"text":" tellraw example with ","color":"yellow"},{"text":"tooltip","color":"yellow","hoverEvent":{"action":"show_text","value":"Hey look at this"}},{"text":" and ","color":"yellow"},{"text":"command","color":"yellow","clickEvent":{"action":"run_command","value":"/kill"}}]`.

### How do I use the JSON Format?

The JSON format revolves around **clusters**. You can create a new cluster like so:
`this is part 1.||part 2 - a new cluser!`

Clusters aren't useful unless you add something to them. These are the available JSON tags:
* `ttp:` - adds a *tooltip*. If a player moves their cursor over the tooltip, they will see the specified text.
* `cmd:` - adds a command. If a player clicks the text, they will run the specified command.
* `sgt:` - adds a suggestion. If a player clicks the text, the specified text will enter their chat input.
* `url:` - adds a URL. If a player clicks the text, it will take them to the URL.

Take a look at this:

```
"&7&oHello, this is a &bsample json&7.||ttp:&bI'm a tooltip for the first cluster.|| There's no tag, so I've started a new cluster.||cmd:/ping||ttp:&6&o&lCLICK&f for /ping")
||___________________________________|  |________________________________________||||_____________________________________________|  |_______|  |_________________________||
|            Average Text                                Tooltip                  ||                 Average Text                   Run Command           Tooltip          |
|_________________________________________________________________________________||_______________________________________________________________________________________|
                                  JSON Cluster #1                                                                           JSON Cluster #2
```
Results in:

![Result_image1](https://i.imgur.com/xlnlX02.png)
![Result_image2](https://i.imgur.com/FSAgQUJu.png)

Note that JSON messages are only for chat. Putting them in kick messages, for example, will have no effect.

### Escaping the JSON syntax

To escape double pipes, double them (use quadruple pipes):

`This is a double pipe: ||||` results in `This is a double pipe: ||`