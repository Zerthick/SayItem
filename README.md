# SayItem

A Simple Minecraft Plugin to Display an item in chat!  Any player with the `sayitem.chat` permission will automatically have their chat messages scanned and replaced. To display an item simply hold the item in your main hand and include `[item]` in your chat message. The placeholder will be replaced with the name of the item and on hover will display the tooltip for it.  The name, color, and formatting are taken directly from the item, so items with custom names and colors will display just fine! 

In addition, **SayItem v1.1.0** added the concept of an _Indexed Placeholder_, `[item]` will still reference the item currently in the player's main hand, however `[item1]` will reference the item in the first slot of the player's hotbar (if present), `[item2]` will reference the second slot. In this way, you can reference multiple items in the same message!

Finally if opting the use the `/sayitem` command there exists a `-j` flag to enable formatting placeholders as consise JSON. For example if I wanted to display a _Poition of Weakness_ in chat but I did not have one in my inventory I could execute:
```
/sayitem -j Anyone want a [{"ContentVersion":1,"Count":1,"ItemType":"minecraft:potion","UnsafeDamage":0,"UnsafeData":{"Potion":"minecraft:weakness"}}]?
``` 
Don't know how to format an item in concise JSON? Check out one of my other plugins [SerializeMe](https://github.com/Zerthick/SerializeMe) :smile:
## Commands

* `/sayitem [-j] <Text>` - Displays `Text` in chat with all item placeholders replaced. Use the `-j` flag to enable parsing JSON placeholders

## Permissions

* `sayitem.chat`
* `sayitem.command.say`
* `sayitem.command.flag.say.j`

## Examples

### Basic Command Usage 
(Examples of normal chat functionality and indexed placeholders to come!)
![Raw Command](https://i.imgur.com/Vdd49Nv.png)
![Result Text](https://i.imgur.com/EsTvD0a.png)

## Support Me
I will **never** charge money for the use of my plugins, however they do require a significant amount of work to maintain and update. If you'd like to show your support and buy me a cup of tea sometime (I don't drink that horrid coffee stuff :P) you can do so [here](https://www.paypal.me/zerthick)
