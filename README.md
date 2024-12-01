[![Generic badge](https://img.shields.io/badge/Version-1.0.0-green.svg)](https://github.com/Kaktushose/jda-commands-inspection/releases/latest)
[![Generic badge](https://img.shields.io/badge/Marketplace-Install-green.svg)](https://plugins.jetbrains.com/plugin/25977-jda-commands-inspection)
[![Generic badge](https://img.shields.io/badge/Github--Release-Download-green.svg)](https://github.com/Kaktushose/jda-commands-inspection/releases/latest)
[![license-shield](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)]()
# JDA-Commands Inspection Plugin
### IntelliJ Code Inspection Plugin for [jda-commands](https://github.com/Kaktushose/jda-commands)

## Example:
```java
@Interaction
public class DynamicOptionTest {

    @SlashCommand("button test")
    public void onCommand(CommandEvent event) {
        // marked as an error, because the correct reference would be "onButton"
        event.withButtons("onClick").reply("Click me");
    }

    @Button("Click me")
    public void onButton(ComponentEvent event) {
        event.reply("You clicked me!");
    }

}
```
## Quickfix
### Quickfix options allow you to generate missing code
#### Before Quickfix:
```java
@Interaction
public class DynamicOptionTest {

    @SlashCommand("button test")
    public void onCommand(CommandEvent event) {
        // marked as an error, because no method "onClick" exists yet
        event.withButtons("onClick").reply("Click me");
    }
}
```
#### After Quickfix:
```java
@Interaction
public class DynamicOptionTest {

    @SlashCommand("button test")
    public void onCommand(CommandEvent event) {
        event.withButtons("onClick").reply("Click me");
    }

    @Button()
    public void onClick(ComponentEvent event) {

    }
}
```

