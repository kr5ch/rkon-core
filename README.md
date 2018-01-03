# rkon-core
This is a library for the [Source RCON Protocol](https://developer.valvesoftware.com/wiki/Source_RCON_Protocol), it is intended for raw use; there are no presets or built-in commands.

## Usage
```java
// Connects to 127.0.0.1 on port 27015
Rcon rcon = new Rcon("127.0.0.1", 27015, "mypassword".getBytes());

// Example: On a minecraft server this will list the connected players
String result = rcon.command("list");

// Display the result in the console
System.out.println(result);
```
When connecting to the rcon server, an `AuthenticationException` will be thrown if the password is incorrect.

## Download
The latest packed .jar, ready to use as a library, is available [here](https://github.com/Kronos666/rkon-core/releases/latest).
