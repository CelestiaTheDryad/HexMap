# Hexmap
Hexmap is a project to allow easy play of tabletop RPGs in an online form. It currently contains text chat, a Hexagonal grid containing moveable unit markers, and some supporting commands such as a dice roller.

## Basics

### How to use
The Hexmap jar is capable of launching both the Hexmap client and server. The client is the default launch option (eg. double clicking the jar). The server must be started via command line arguments.

### Command line arguments
server : launches the Hexmap server

### Example usage
java -jar Hexmap-0.4-SNAPSHOT.jar server

### Client functionality
Once connected, a client can enter chat messages, execute /roll commands, and move units around the Hexmap. Moving units is done by clicking on a tile to select a unit, using a dropdown to select if more than one unit is on that tile, then clicking on a destination tile to move the unit to the new location

## Passwords
When a user connects to a server they must enter a username, and may choose to enter a password. **This password is currently stored and transmitted in plain text, do not use any important password here.** If the user enters a password, the server will lock their username to that password if that user has not been registered yet. If the username is already registered, the given password must match the password stored by the server or the connection will be refused.

## Commands

### roll
/roll [private ] <int>d<int>[(+|-)<int>]
rolls a given dice code, eg "3d6", and diplays the result either privately or to all users
hexmap.commands.roll

### add
/add <name> <x> <y> <r> <g> <b>
Adds a new unit to the board with name <name>, position (<x>,<y>), and color (<r>,<g>,<b>)
hexmap.commands.add

### others

## Permissions
The permissions system is used to control which actions a user may or may not take on a server. To edit a user's permissions, edit their file in permissions/user/. A permission is a string of the form "<base1>.<base2>.<key>", where each base indicates a position in a tree structure, and the key is the final name for the permission. Permission may be granted to entire subtrees using the format "<base1>.*", thus a user would have any permission starting with <base1>.
